package name.xupeng.demo.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Properties;

/**
 * Lucene 的场景演示类。
 *
 * @author xupeng 2017-10-19
 */
public class LuceneStoryDemos {

  /**
   * 一个完整的索引和搜索场景。
   */
  @Test
  public void indexAndSearchDemoTest() throws IOException {

    // 指定 Lucene 存放索引数据的位置，这里把索引数据放在内存中
    Directory idxDir = new RAMDirectory();

    try {

      // 创建一个 Lucene 自带的标准分析器，所有被索引的文本会通过该分析器解析为词条
      Analyzer analyzer;
      analyzer = new StandardAnalyzer();
      //analyzer = new SmartChineseAnalyzer();



      // 创建索引写入器配置项，通过配置项把前述的分析器引入索引写入器
      IndexWriterConfig idxWriterCfg = new IndexWriterConfig(analyzer);
      // 创建索引写入器，所有 Lucene 文档通过该写入器编入 Lucene 索引
      final IndexWriter idxWriter = new IndexWriter(idxDir, idxWriterCfg);

      try {
        // 索引原文件目录
        Path docDir = null;
        try {
          docDir = Paths.get(getClass().getClassLoader().getResource("index-files").toURI());
        } catch (URISyntaxException e) {
          e.printStackTrace();
        }

        Files.walkFileTree(docDir, new SimpleFileVisitor<Path>() {

          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

            try {

              try (BufferedReader bf = new BufferedReader(new InputStreamReader(Files.newInputStream(file)))) {
                // 创建 Lucene 文档以备索引
                Document doc = new Document();

                // 以下为文档添加字段
                Field pathField = new StringField("path", file.toString(), Field.Store.YES);
                doc.add(pathField);

                Properties docContent = new Properties();
                docContent.load(bf);

                docContent.forEach((key, value) -> {
                  switch (key.toString()) {
                    case "score":
                      Field floatField = new FloatDocValuesField("score", Float.valueOf(value.toString()));
                      doc.add(floatField);
                      break;
                    default:
                      Field textField = new TextField(key.toString(), value.toString(), Field.Store.YES);
                      doc.add(textField);
                  }
                });

                // 写 Lucene 文档到索引
                idxWriter.addDocument(doc);
              }

            } catch (IOException ignore) {

            }

            return FileVisitResult.CONTINUE;
          }

        });
      } finally {
        idxWriter.close();
      }



      IndexReader idxReader = DirectoryReader.open(idxDir);

      try {

        System.out.println("indexed doc num: " + idxReader.numDocs());

        // 创建搜索器
        IndexSearcher searcher = new IndexSearcher(idxReader);
        TopDocs topDocs;

        QueryParser parser = new QueryParser("ti", analyzer);
        Query q;

        q = parser.parse("装备");
        topDocs = searcher.search(q, 2);
        showSearchResutl(q, searcher, topDocs);

        q = parser.parse("\"装备\" AND 蔬菜");
        topDocs = searcher.search(q, 2);
        showSearchResutl(q, searcher, topDocs);

        q = parser.parse("ab:car");
        topDocs = searcher.search(q, 2);
        showSearchResutl(q, searcher, topDocs);

        q = parser.parse("ad:[20170101 TO 20171231]");
        topDocs = searcher.search(q, 2);
        showSearchResutl(q, searcher, topDocs);

        q = parser.parse("ad:2016*");
        topDocs = searcher.search(q, 3);
        showSearchResutl(q, searcher, topDocs);

      } catch (ParseException e) {
        e.printStackTrace();
      } finally {
        idxReader.close();
      }

    } finally {
      idxDir.close();
    }
  }

  private void showSearchResutl(Query q, IndexSearcher searcher, TopDocs result) throws IOException {
    System.out.println("\nsearch: " + q.toString());
    System.out.println("hit doc count: " + result.totalHits);
    for (ScoreDoc scoreDoc : result.scoreDocs) {
      Document document = searcher.doc(scoreDoc.doc);
      System.out.println(scoreDoc.doc + ": " + document.get("ti") + ": " + document.get("path"));
    }
  }
}
