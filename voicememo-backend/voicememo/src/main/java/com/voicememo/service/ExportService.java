package com.voicememo.service;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.voicememo.model.ActionItem;
import com.voicememo.model.Memo;
import com.voicememo.repository.MemoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {

    private final MemoRepository memoRepository;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMMM dd, yyyy HH:mm");

    // ─── TXT Export ──────────────────────────────────────────────────────────

    public byte[] exportAsTxt(Long memoId) {
        Memo memo = getMemo(memoId);
        StringBuilder sb = new StringBuilder();

        sb.append("=".repeat(60)).append("\n");
        sb.append(memo.getTitle().toUpperCase()).append("\n");
        sb.append("=".repeat(60)).append("\n\n");

        sb.append("Date:     ").append(memo.getCreatedAt().format(DATE_FMT)).append("\n");

        if (memo.getDurationSeconds() != null) {
            sb.append("Duration: ").append(formatDuration(memo.getDurationSeconds())).append("\n");
        }
        if (memo.getWordCount() != null) {
            sb.append("Words:    ").append(memo.getWordCount()).append("\n");
        }
        if (memo.getDetectedLanguage() != null) {
            sb.append("Language: ").append(memo.getDetectedLanguage()).append("\n");
        }

        sb.append("\n");

        if (memo.getSummary() != null && !memo.getSummary().isBlank()) {
            sb.append("SUMMARY\n");
            sb.append("-".repeat(40)).append("\n");
            sb.append(memo.getSummary()).append("\n\n");
        }

        if (memo.getTranscription() != null && !memo.getTranscription().isBlank()) {
            sb.append("TRANSCRIPTION\n");
            sb.append("-".repeat(40)).append("\n");
            sb.append(memo.getTranscription()).append("\n\n");
        }

        if (memo.getTags() != null && !memo.getTags().isEmpty()) {
            sb.append("TAGS\n");
            sb.append("-".repeat(40)).append("\n");
            memo.getTags().forEach(t -> sb.append("  #").append(t.getName()).append("\n"));
            sb.append("\n");
        }

        if (memo.getActionItems() != null && !memo.getActionItems().isEmpty()) {
            sb.append("ACTION ITEMS\n");
            sb.append("-".repeat(40)).append("\n");
            memo.getActionItems().forEach(a -> {
                String checkbox = a.isCompleted() ? "[x]" : "[ ]";
                String priority = "[" + a.getPriority().name() + "]";
                sb.append("  ").append(checkbox).append(" ")
                        .append(priority).append(" ").append(a.getText()).append("\n");
                if (a.getDeadlineHint() != null) {
                    sb.append("       Deadline: ").append(a.getDeadlineHint()).append("\n");
                }
            });
        }

        sb.append("\n").append("=".repeat(60)).append("\n");
        sb.append("Exported by VoiceMemo App\n");

        return sb.toString().getBytes();
    }

    // ─── PDF Export ──────────────────────────────────────────────────────────

    public byte[] exportAsPdf(Long memoId) throws IOException {
        Memo memo = getMemo(memoId);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document doc = new Document(pdfDoc);

        PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont mono = PdfFontFactory.createFont(StandardFonts.COURIER);

        // Title
        doc.add(new Paragraph(memo.getTitle())
                .setFont(bold)
                .setFontSize(20)
                .setTextAlignment(TextAlignment.LEFT)
                .setMarginBottom(4));

        // Metadata row
        String meta = memo.getCreatedAt().format(DATE_FMT);
        if (memo.getWordCount() != null) meta += "  ·  " + memo.getWordCount() + " words";
        if (memo.getDurationSeconds() != null) meta += "  ·  " + formatDuration(memo.getDurationSeconds());

        doc.add(new Paragraph(meta)
                .setFont(regular)
                .setFontSize(10)
                .setFontColor(ColorConstants.GRAY)
                .setMarginBottom(16));

        // Tags
        if (memo.getTags() != null && !memo.getTags().isEmpty()) {
            String tagLine = memo.getTags().stream()
                    .map(t -> "#" + t.getName())
                    .reduce((a, b) -> a + "  " + b)
                    .orElse("");
            doc.add(new Paragraph(tagLine)
                    .setFont(regular)
                    .setFontSize(10)
                    .setFontColor(ColorConstants.BLUE)
                    .setMarginBottom(16));
        }

        // Summary section
        if (memo.getSummary() != null && !memo.getSummary().isBlank()) {
            doc.add(sectionHeader("Summary", bold));
            doc.add(new Paragraph(memo.getSummary())
                    .setFont(regular)
                    .setFontSize(11)
                    .setMarginBottom(16));
        }

        // Transcription section
        if (memo.getTranscription() != null && !memo.getTranscription().isBlank()) {
            doc.add(sectionHeader("Transcription", bold));
            doc.add(new Paragraph(memo.getTranscription())
                    .setFont(mono)
                    .setFontSize(10)
                    .setMarginBottom(16));
        }

        // Action items section
        if (memo.getActionItems() != null && !memo.getActionItems().isEmpty()) {
            doc.add(sectionHeader("Action Items", bold));
            memo.getActionItems().forEach(a -> {
                String checkbox = a.isCompleted() ? "☑ " : "☐ ";
                String line = checkbox + "[" + a.getPriority().name() + "] " + a.getText();
                if (a.getDeadlineHint() != null) line += "  —  " + a.getDeadlineHint();

                doc.add(new Paragraph(line)
                        .setFont(regular)
                        .setFontSize(10)
                        .setMarginBottom(4));
            });
        }

        // Footer
        doc.add(new Paragraph("\nExported by VoiceMemo App")
                .setFont(regular)
                .setFontSize(8)
                .setFontColor(ColorConstants.LIGHT_GRAY)
                .setTextAlignment(TextAlignment.CENTER));

        doc.close();
        return baos.toByteArray();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Memo getMemo(Long id) {
        return memoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Memo not found: " + id));
    }

    private Paragraph sectionHeader(String title, PdfFont bold) throws IOException {
        return new Paragraph(title)
                .setFont(bold)
                .setFontSize(13)
                .setMarginTop(8)
                .setMarginBottom(6);
    }

    private String formatDuration(int seconds) {
        int mins = seconds / 60;
        int secs = seconds % 60;
        return mins > 0
                ? String.format("%dm %ds", mins, secs)
                : String.format("%ds", secs);
    }
}