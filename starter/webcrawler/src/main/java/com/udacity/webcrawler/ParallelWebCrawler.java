package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Provider;

/**
 * A concrete implementation of {@link WebCrawler} that runs multiple threads on a
 * {@link ForkJoinPool} to fetch and process multiple web pages in parallel.
 */
final class ParallelWebCrawler implements WebCrawler {

    private final Clock clock;
    private final Duration timeout;
    private final int popularWordCount;
    private final ForkJoinPool pool;
    private final PageParserFactory parserFactory;
    private final List<Pattern> ignoredUrls;
    private final int maxDepth;

    @Inject
    ParallelWebCrawler(
        Clock clock,
        @Timeout Duration timeout,
        @PopularWordCount int popularWordCount,
        @TargetParallelism int threadCount,
        PageParserFactory parserFactory,
        @IgnoredUrls List<Pattern> ignoredUrls,
        @MaxDepth int maxDepth
    ) {
        this.clock = clock;
        this.timeout = timeout;
        this.popularWordCount = popularWordCount;

        this.parserFactory = parserFactory;
        this.ignoredUrls = ignoredUrls;
        this.maxDepth = maxDepth;

        this.pool = new ForkJoinPool(
            Math.min(threadCount, getMaxParallelism())
        );
    }

    @Override
    public CrawlResult crawl(List<String> startingUrls) {
        Instant deadline = clock.instant().plus(timeout);
        // Normal Hashmaps does gives random output due to race condition
        ConcurrentMap<String, Integer> counts = new ConcurrentHashMap<>();
        Set<String> visitedUrls = ConcurrentHashMap.newKeySet();

        List<CrawlTask> tasks = startingUrls
            .stream()
            .map(url ->
                new CrawlTask(url, maxDepth, deadline, counts, visitedUrls)
            )
            .collect(Collectors.toList());

        pool.invoke(
            new RecursiveAction() {
                @Override
                protected void compute() {
                    invokeAll(tasks);
                }
            }
        );

        return new CrawlResult.Builder()
            .setWordCounts(WordCounts.sort(counts, popularWordCount))
            .setUrlsVisited(visitedUrls.size())
            .build();
    }

    @Override
    public int getMaxParallelism() {
        return Runtime.getRuntime().availableProcessors();
    }

    private final class CrawlTask extends RecursiveAction {

        private final String url;
        private final int depth;
        private final Instant deadline;
        private final ConcurrentMap<String, Integer> counts;
        private final Set<String> visited;

        CrawlTask(
            String url,
            int depth,
            Instant deadline,
            ConcurrentMap<String, Integer> counts,
            Set<String> visit
        ) {
            this.url = url;
            this.depth = depth;
            this.deadline = deadline;
            this.counts = counts;
            this.visited = visit;
        }

        @Override
        protected void compute() {
            if (depth == 0) {
                return;
            }
            if (clock.instant().isAfter(deadline)) {
                return;
            }
            for (Pattern pattern : ignoredUrls) {
                if (pattern.matcher(url).matches()) {
                    return;
                }
            }

            if (!visited.add(url)) {
                return;
            }
            PageParser.Result result = parserFactory.get(url).parse();
            result
                .getWordCounts()
                .forEach((word, count) ->
                    counts.merge(word, count, Integer::sum)
                );

            List<CrawlTask> subtasks = result
                .getLinks()
                .stream()
                .map(link ->
                    new CrawlTask(link, depth - 1, deadline, counts, visited)
                )
                .collect(Collectors.toList());
            invokeAll(subtasks);
        }
    }
}
