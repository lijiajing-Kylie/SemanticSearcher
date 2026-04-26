# SemanticSearcher

A high-performance concurrent search engine built in Java, featuring BM25-based relevance ranking, an inverted index backed by SQLite, and a modern dark-themed GUI.

## Demo

- Search `computer science` → returns 10 ranked results in ~51ms
- Search `machine learning` → top result scores 0.8072, clear score separation between results
- Click any result card to open the full Wikipedia article in your browser

## Features

- Multi-threaded web crawler using `ExecutorService` + `BlockingQueue` (producer-consumer pattern)
- Crawled and indexed **1,000 Wikipedia pages**
- NLP pipeline: tokenization, stop-word removal, TF normalization
- Inverted index persisted in SQLite with optimized term indexing
- BM25 probabilistic ranking with Top-K selection via `PriorityQueue`
- Batch SQL queries to minimize DB round-trips
- Clickable result cards — opens Wikipedia article in browser
- Swing-based GUI with asynchronous search (non-blocking UI thread)
- 15 unit tests covering NLP, ranking correctness, and DB integrity

## Architecture

Acquisition Layer → Processing Layer → Storage Layer → Service Layer
WikiCrawler        TextProcessor      DatabaseManager   SearchEngine
IndexBuilder                         SearchGUI

## Tech Stack

Java 17+ · SQLite · JDBC · Jsoup · Swing · JUnit 5 · Maven

## Quick Start

1. Clone the repo
2. Run `WikiCrawler` — crawls 1,000 Wikipedia pages into `wiki_pages/`
3. Run `IndexBuilder` — builds inverted index in SQLite
4. Run `SearchGUI` — launches the search interface

## Performance

- Dataset: 1,000 Wikipedia pages
- Query response time: ~20–51ms
- Batch SQL optimization reduces DB round-trips from O(N×M) to O(1)
- Term field indexed in SQLite for fast lookup
- 15 unit tests, all passing