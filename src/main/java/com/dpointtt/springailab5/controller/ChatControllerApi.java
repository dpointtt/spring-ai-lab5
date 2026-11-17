package com.dpointtt.springailab5.controller;

import com.dpointtt.springailab5.model.SaveArticleToEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Controller
public class ChatControllerApi {

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private static final String PROMPT = """
            Ти — сервіс перетворення JSON-даних.
            Вхідні дані — неструктурований опис (бібліографічне цитування) наукової статті.
            Сформуй результат строго у форматі JSON: один JSON-об'єкт без пояснень, без markdown-розмітки і без жодного тексту до чи після нього.
            Усі значення мають бути рядками. Не використовуй масиви та вкладені об'єкти.

            Загальні правила:
            1. Не вигадуй дані, яких немає у вхідному тексті.
            2. Якщо текстове поле не знайдено — став "" (порожній рядок).
            3. Якщо числове або логічне поле не знайдено чи його неможливо визначити — став "0".
            4. Використовуй назви ключів точно такі, як наведено нижче, не додавай інших ключів.

            Ключі та правила заповнення:
            title_of_the_article — назва статті без прізвищ авторів, без назви журналу та без крапки в кінці;
            author_set — усі автори так, як вони вказані в тексті, через кому;
            first_author — перший автор;
            number_of_authors — кількість авторів числом;
            year_of_publication — рік публікації (4 цифри);
            name_journal — назва журналу без лапок;
            journal_volume — номер тому (лише число);
            journal_issue — номер випуску (лише число);
            place_of_publication — місто видання;
            country_of_publication — країна видання; вказуй її лише якщо вона прямо названа або місто однозначно належить певній країні;
            publisher — видавець;
            pages_range — діапазон сторінок у форматі "10-20";
            first_page — номер першої сторінки;
            last_page — номер останньої сторінки;
            total_volume_of_printed_sheets — кількість сторінок, обчислена за формулою last_page - first_page + 1; якщо сторінки невідомі — "0";
            doi — DOI статті;
            issn — ISSN журналу;
            url — посилання на статтю;
            language — мова статті за її назвою: "ukr" — українська, "eng" — англійська, "other" — будь-яка інша;
            published_in_ukraine — "1", якщо видання вийшло в Україні (за містом чи країною у тексті), інакше "0";
            published_abroad — "1", якщо видання вийшло за межами України, інакше "0"; обидва прапорці не можуть бути "1" одночасно, а якщо місце видання невідоме — обидва "0";
            specialized — завжди "Category A";
            scopus_indexed — "1" лише якщо Scopus прямо згадано в тексті, інакше "0";
            web_of_science_indexed — "1" лише якщо Web of Science прямо згадано в тексті, інакше "0";
            keywords — ключові слова через кому, лише якщо вони є в тексті;
            abstract_text — анотація, лише якщо вона є в тексті;
            udc — індекс УДК;
            article_type — тип публікації (стаття, тези, огляд), лише якщо він вказаний;
            affiliation — установа автора;
            co_authors — усі автори, крім першого;
            conference_name — назва конференції, якщо публікація є матеріалами конференції;
            grant_info — інформація про грант чи фінансування.

            Вхідний текст:
            {message}
            """;

    private final OpenAiChatModel chatModel;
    private final ObjectMapper objectMapper;

    public ChatControllerApi(OpenAiChatModel chatModel, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/generate-all")
    public String generateAll(@RequestParam("message") String message, Model model) {
        model.addAttribute("message", message);

        if (message == null || message.isBlank()) {
            model.addAttribute("error", "Введіть текст статті.");
            return "index";
        }

        long start = System.nanoTime();
        SaveArticleToEntity article;

        try {
            Prompt prompt = new PromptTemplate(PROMPT).create(Map.of("message", message));
            String raw = chatModel.call(prompt).getResult().getOutput().getText();
            model.addAttribute("rawResponse", raw);

            article = objectMapper.readValue(cleanJson(raw), SaveArticleToEntity.class);
        } catch (JsonProcessingException e) {
            log.warn("AI повернув некоректний JSON", e);
            model.addAttribute("error", "Модель повернула некоректний JSON: " + e.getOriginalMessage());
            article = SaveArticleToEntity.empty();
        } catch (RuntimeException e) {
            log.error("Помилка виклику AI", e);
            model.addAttribute("error", "Помилка звернення до AI: " + e.getMessage());
            article = SaveArticleToEntity.empty();
        }

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        model.addAttribute("generatedAt", LocalDateTime.now().format(CLOCK));
        model.addAttribute("elapsedMs", elapsedMs);
        model.addAttribute("fields", objectMapper.convertValue(article, new TypeReference<LinkedHashMap<String, String>>() {}));
        model.addAttribute("json", objectMapper.valueToTree(article).toPrettyString());
        return "index";
    }

    static String cleanJson(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim()
                .replaceAll("(?s)^```(?:json)?\\s*", "")
                .replaceAll("(?s)\\s*```$", "")
                .trim();
        int from = s.indexOf('{');
        int to = s.lastIndexOf('}');
        return (from >= 0 && to > from) ? s.substring(from, to + 1) : s;
    }
}
