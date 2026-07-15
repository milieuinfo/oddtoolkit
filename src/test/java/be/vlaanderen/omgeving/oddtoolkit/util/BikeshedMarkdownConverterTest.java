package be.vlaanderen.omgeving.oddtoolkit.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for BikeshedMarkdownConverter using flexmark-java.
 */
class BikeshedMarkdownConverterTest {

  @Test
  void convertsMarkdownWithTables() {
    String markdown = """
        ## Introduction
        
        This is a test paragraph with **bold** and *italic* text.
        
        ### Table Example
        
        | Column 1 | Column 2 | Column 3 |
        |----------|----------|----------|
        | Value A  | Value B  | Value C  |
        | Value D  | Value E  | Value F  |
        """;

    String result = BikeshedMarkdownConverter.convert(markdown, "Test Section");

    // Verify Bikeshad heading wrapper
    assertThat(result).contains("## Test Section ## {#test-section}");
    
    // Verify GFM table is rendered with class="data"
    assertThat(result).contains("<table class=\"data\">");
    assertThat(result).contains("<thead>");
    assertThat(result).contains("<th>Column 1</th>");
    assertThat(result).contains("<tbody>");
    assertThat(result).contains("<td>Value A</td>");
    
    // Verify inline formatting
    assertThat(result).contains("<strong>bold</strong>");
    assertThat(result).contains("<em>italic</em>");
  }

  @Test
  void convertsHeadingsToBikeshadSyntax() {
    String markdown = """
        # Heading 1
        
        ## Heading 2
        
        ### Heading 3
        """;

    String result = BikeshedMarkdownConverter.convert(markdown, null);

    // All headings should be converted to Bikeshad syntax with anchors
    // Note: heading level is preserved from original markdown (h1=#, h2=##, h3=###)
    assertThat(result).contains("# Heading 1 # {#heading-1}");
    assertThat(result).contains("## Heading 2 ## {#heading-2}");
    assertThat(result).contains("### Heading 3 ### {#heading-3}");
  }

  @Test
  void convertsCodeBlocks() {
    String markdown = """
        ```java
        public class Test {
            public static void main(String[] args) {
                System.out.println("Hello");
            }
        }
        ```
        """;

    String result = BikeshedMarkdownConverter.convert(markdown, null);

    assertThat(result).contains("<pre><code");
    assertThat(result).contains("public class Test");
    assertThat(result).contains("System.out.println");
  }

  @Test
  void convertsLists() {
    String markdown = """
        - Item 1
        - Item 2
        - Item 3
        
        1. First
        2. Second
        3. Third
        """;

    String result = BikeshedMarkdownConverter.convert(markdown, null);

    assertThat(result).contains("<ul>");
    assertThat(result).contains("<li>Item 1</li>");
    assertThat(result).contains("</ul>");
    assertThat(result).contains("<ol>");
    assertThat(result).contains("<li>First</li>");
    assertThat(result).contains("</ol>");
  }

  @Test
  void convertsBlockquotes() {
    String markdown = """
        > This is a quote
        > with multiple lines""";

    String result = BikeshedMarkdownConverter.convert(markdown, null);

    assertThat(result).contains("<blockquote>");
    assertThat(result).contains("This is a quote");
    assertThat(result).contains("</blockquote>");
  }

  @Test
  void handlesEmptyInput() {
    assertThat(BikeshedMarkdownConverter.convert(null)).isEmpty();
    assertThat(BikeshedMarkdownConverter.convert("")).isEmpty();
    assertThat(BikeshedMarkdownConverter.convert(null, "Title")).isEmpty();
    assertThat(BikeshedMarkdownConverter.convert("", "Title")).isEmpty();
  }

  @Test
  void handlesSectionTitle() {
    String markdown = "Some paragraph text.";
    String result = BikeshedMarkdownConverter.convert(markdown, "My Section");

    assertThat(result).startsWith("## My Section ## {#my-section}");
    assertThat(result).contains("<p>Some paragraph text.</p>");
  }

  @Test
  void escapesHtmlInHeadings() {
    String markdown = "## Heading with <script>alert('xss')</script>";
    String result = BikeshedMarkdownConverter.convert(markdown, null);

    assertThat(result).contains("&lt;script&gt;");
    assertThat(result).doesNotContain("<script>");
  }

  @Test
  void convertsLinks() {
    String markdown = "Check [this link](https://example.com) for more.";

    String result = BikeshedMarkdownConverter.convert(markdown, null);

    assertThat(result).contains("<a href=\"https://example.com\">this link</a>");
  }

  @Test
  void handlesGfmStrikethrough() {
    String markdown = "This is ~~deleted~~ text.";

    String result = BikeshedMarkdownConverter.convert(markdown, null);

    assertThat(result).contains("<del>deleted</del>");
  }
}
