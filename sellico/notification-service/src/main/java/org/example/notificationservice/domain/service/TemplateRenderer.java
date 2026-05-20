package org.example.notificationservice.domain.service;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

/**
 * Domain service for rendering templates with Mustache
 */
@Service
public class TemplateRenderer {

    private final MustacheFactory mustacheFactory;

    public TemplateRenderer() {
        this.mustacheFactory = new DefaultMustacheFactory();
    }

    /**
     * Render a template string with the given context
     * @param template the template string (e.g., "Hello {{name}}")
     * @param context the context map with values
     * @return the rendered string
     */
    public String render(String template, Map<String, Object> context) {
        if (template == null || template.isEmpty()) {
            return "";
        }

        Mustache mustache = mustacheFactory.compile(new StringReader(template), "template");
        StringWriter writer = new StringWriter();
        mustache.execute(writer, context);
        return writer.toString();
    }
}

