package com.dpointtt.springailab5.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SaveArticleToEntity(
        String title_of_the_article,
        String author_set,
        String first_author,
        String number_of_authors,
        String year_of_publication,
        String name_journal,
        String journal_volume,
        String journal_issue,
        String place_of_publication,
        String country_of_publication,
        String publisher,
        String pages_range,
        String first_page,
        String last_page,
        String total_volume_of_printed_sheets,
        String doi,
        String issn,
        String url,
        String language,
        String published_in_ukraine,
        String published_abroad,
        String specialized,
        String scopus_indexed,
        String web_of_science_indexed,
        String keywords,
        String abstract_text,
        String udc,
        String article_type,
        String affiliation,
        String co_authors,
        String conference_name,
        String grant_info
) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public SaveArticleToEntity {
        title_of_the_article = text(title_of_the_article);
        author_set = text(author_set);
        first_author = text(first_author);
        number_of_authors = num(number_of_authors);
        year_of_publication = num(year_of_publication);
        name_journal = text(name_journal);
        journal_volume = num(journal_volume);
        journal_issue = num(journal_issue);
        place_of_publication = text(place_of_publication);
        country_of_publication = text(country_of_publication);
        publisher = text(publisher);
        pages_range = text(pages_range);
        first_page = num(first_page);
        last_page = num(last_page);
        total_volume_of_printed_sheets = num(total_volume_of_printed_sheets);
        doi = text(doi);
        issn = text(issn);
        url = text(url);
        language = isBlank(language) ? "other" : language.trim();
        published_in_ukraine = num(published_in_ukraine);
        published_abroad = num(published_abroad);
        specialized = isBlank(specialized) ? "Category A" : specialized.trim();
        scopus_indexed = num(scopus_indexed);
        web_of_science_indexed = num(web_of_science_indexed);
        keywords = text(keywords);
        abstract_text = text(abstract_text);
        udc = text(udc);
        article_type = text(article_type);
        affiliation = text(affiliation);
        co_authors = text(co_authors);
        conference_name = text(conference_name);
        grant_info = text(grant_info);
    }

    public static SaveArticleToEntity empty() {
        try {
            return MAPPER.readValue("{}", SaveArticleToEntity.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean isBlank(String v) {
        return v == null || v.isBlank();
    }

    private static String text(String v) {
        return isBlank(v) ? "" : v.trim();
    }

    private static String num(String v) {
        return isBlank(v) ? "0" : v.trim();
    }
}