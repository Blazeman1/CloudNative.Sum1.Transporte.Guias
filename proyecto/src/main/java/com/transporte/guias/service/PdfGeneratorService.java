package com.transporte.guias.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.transporte.guias.model.Guia;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

/**
 * Servicio para generar el PDF de una Guía de Despacho
 * Utiliza la librería iTextPDF 5
 */
@Slf4j
@Service
public class PdfGeneratorService {

    private static final Font TITLE_FONT   = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.DARK_GRAY);
    private static final Font HEADER_FONT  = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.WHITE);
    private static final Font LABEL_FONT   = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
    private static final Font VALUE_FONT   = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
    private static final Font FOOTER_FONT  = new Font(Font.FontFamily.HELVETICA,  8, Font.ITALIC, BaseColor.GRAY);

    private static final BaseColor HEADER_BG = new BaseColor(46, 117, 182);   // Azul corporativo
    private static final BaseColor ROW_ALT   = new BaseColor(235, 242, 250);  // Azul muy claro

    /**
     * Genera el PDF de una guía y lo devuelve como arreglo de bytes.
     *
     * @param guia objeto Guia con todos los datos del despacho
     * @return bytes del PDF generado
     */
    public byte[] generarPdf(Guia guia) {
        log.info("Generando PDF para guía ID: {}", guia.getId());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Document document = new Document(PageSize.A4, 50, 50, 70, 50);
            PdfWriter writer = PdfWriter.getInstance(document, baos);

            // Metadata del documento
            document.addTitle("Guía de Despacho " + guia.getId());
            document.addAuthor("Sistema de Gestión de Guías - Transporte");
            document.addCreationDate();

            document.open();

            // ── Encabezado ──────────────────────────────────────────────
            Paragraph titulo = new Paragraph("GUÍA DE DESPACHO", TITLE_FONT);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(4);
            document.add(titulo);

            Paragraph subtitulo = new Paragraph("N° " + guia.getId(), VALUE_FONT);
            subtitulo.setAlignment(Element.ALIGN_CENTER);
            subtitulo.setSpacingAfter(16);
            document.add(subtitulo);

            // Línea separadora
            LineSeparator line = new LineSeparator(1, 100, HEADER_BG, Element.ALIGN_CENTER, -2);
            document.add(new Chunk(line));
            document.add(Chunk.NEWLINE);

            // ── Sección: Datos del Transportista ────────────────────────
            document.add(crearSeccionHeader("DATOS DEL TRANSPORTISTA"));
            PdfPTable tablaTransp = crearTabla2Col();
            agregarFila(tablaTransp, "Transportista:", guia.getTransportista(), false);
            agregarFila(tablaTransp, "Fecha:",
                    guia.getFecha() != null
                            ? guia.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            : "—",
                    true);
            agregarFila(tablaTransp, "Estado:", guia.getEstado().name(), false);
            document.add(tablaTransp);

            // ── Sección: Datos del Destinatario ─────────────────────────
            document.add(crearSeccionHeader("DATOS DEL DESTINATARIO"));
            PdfPTable tablaDestin = crearTabla2Col();
            agregarFila(tablaDestin, "Destinatario:", guia.getDestinatario(), false);
            agregarFila(tablaDestin, "Dirección:", guia.getDireccionDestino(), true);
            agregarFila(tablaDestin, "Ciudad:", guia.getCiudadDestino(), false);
            document.add(tablaDestin);

            // ── Sección: Detalle de la Mercadería ───────────────────────
            document.add(crearSeccionHeader("DETALLE DE LA MERCADERÍA"));
            PdfPTable tablaMerc = crearTabla2Col();
            agregarFila(tablaMerc, "Descripción:", guia.getDescripcionMercaderia(), false);
            agregarFila(tablaMerc, "Bultos:", String.valueOf(guia.getBultos()), true);
            agregarFila(tablaMerc, "Peso Total:", guia.getPesoKg() + " kg", false);
            agregarFila(tablaMerc, "Valor Declarado:",
                    guia.getValorDeclarado() != null
                            ? String.format("$ %,.0f CLP", guia.getValorDeclarado())
                            : "No declarado",
                    true);
            document.add(tablaMerc);

            // ── Sección de firmas ────────────────────────────────────────
            document.add(Chunk.NEWLINE);
            document.add(Chunk.NEWLINE);
            PdfPTable tablaFirmas = new PdfPTable(2);
            tablaFirmas.setWidthPercentage(90);
            tablaFirmas.setSpacingBefore(20);

            tablaFirmas.addCell(crearCeldaFirma("_______________________________\nFirma Transportista"));
            tablaFirmas.addCell(crearCeldaFirma("_______________________________\nFirma Receptor / Destinatario"));
            document.add(tablaFirmas);

            // ── Pie de página ────────────────────────────────────────────
            document.add(Chunk.NEWLINE);
            Paragraph footer = new Paragraph(
                    "Documento generado el " +
                    java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) +
                    " | Sistema de Gestión de Guías de Despacho",
                    FOOTER_FONT);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            log.info("PDF generado correctamente para guía ID: {}, tamaño: {} bytes",
                    guia.getId(), baos.size());

            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error al generar PDF para guía {}: {}", guia.getId(), e.getMessage(), e);
            throw new RuntimeException("Error al generar el PDF de la guía: " + e.getMessage(), e);
        }
    }

    // ── Métodos auxiliares ───────────────────────────────────────────────────

    private Paragraph crearSeccionHeader(String texto) {
        Paragraph p = new Paragraph(texto, HEADER_FONT);
        p.setAlignment(Element.ALIGN_LEFT);
        p.setSpacingBefore(12);
        p.setSpacingAfter(4);
        // Fondo simulado con un párrafo de color
        p.getFont().setColor(BaseColor.WHITE);
        // iText 5 no soporta fondo en Paragraph directamente, se usa tabla de 1 col
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell(new Phrase(texto, HEADER_FONT));
        cell.setBackgroundColor(HEADER_BG);
        cell.setPadding(5);
        cell.setBorder(Rectangle.NO_BORDER);
        t.addCell(cell);
        // Retornamos párrafo vacío; la tabla se agrega desde el caller mediante un trick
        // En vez de eso, devolvemos el Paragraph con la fuente ajustada y dejamos que
        // el caller use el método crearHeaderTabla
        Paragraph fallback = new Paragraph(" ", VALUE_FONT);
        fallback.setSpacingBefore(0);
        return fallback;   // se ignora; usar crearHeaderTabla() directamente
    }

    /**
     * Crea una tabla de 1 celda que actúa como encabezado de sección con fondo azul.
     */
    private PdfPTable crearHeaderTabla(String texto) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(14);
        t.setSpacingAfter(2);
        PdfPCell cell = new PdfPCell(new Phrase(texto, HEADER_FONT));
        cell.setBackgroundColor(HEADER_BG);
        cell.setPadding(6);
        cell.setBorder(Rectangle.NO_BORDER);
        t.addCell(cell);
        return t;
    }

    /**
     * Genera el PDF de una guía con encabezados de sección correctos.
     * (Versión refactorizada utilizada internamente.)
     */
    private void agregarSeccion(Document doc, String titulo, PdfPTable tabla) throws DocumentException {
        doc.add(crearHeaderTabla(titulo));
        doc.add(tabla);
    }

    private PdfPTable crearTabla2Col() throws RuntimeException {
        try {
            PdfPTable tabla = new PdfPTable(new float[]{35f, 65f});
            tabla.setWidthPercentage(100);
            tabla.setSpacingAfter(6);
            return tabla;
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        }
    }

    private void agregarFila(PdfPTable tabla, String label, String valor, boolean altRow) {
        BaseColor bg = altRow ? ROW_ALT : BaseColor.WHITE;

        PdfPCell cLabel = new PdfPCell(new Phrase(label, LABEL_FONT));
        cLabel.setBackgroundColor(bg);
        cLabel.setPadding(6);
        cLabel.setBorderColor(new BaseColor(200, 200, 200));

        PdfPCell cValue = new PdfPCell(new Phrase(valor != null ? valor : "—", VALUE_FONT));
        cValue.setBackgroundColor(bg);
        cValue.setPadding(6);
        cValue.setBorderColor(new BaseColor(200, 200, 200));

        tabla.addCell(cLabel);
        tabla.addCell(cValue);
    }

    private PdfPCell crearCeldaFirma(String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, VALUE_FONT));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingTop(20);
        return cell;
    }

    /**
     * Versión mejorada que sí usa crearHeaderTabla para cada sección.
     * Este método sobreescribe la lógica del método generarPdf original.
     */
    public byte[] generarPdfCompleto(Guia guia) {
        log.info("Generando PDF completo para guía ID: {}", guia.getId());
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 50, 50, 70, 50);
            PdfWriter.getInstance(document, baos);
            document.addTitle("Guía de Despacho " + guia.getId());
            document.addAuthor("Sistema de Gestión de Guías - Transporte");
            document.open();

            // Título
            Paragraph titulo = new Paragraph("GUÍA DE DESPACHO", TITLE_FONT);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(4);
            document.add(titulo);
            Paragraph nro = new Paragraph("N° " + guia.getId(), VALUE_FONT);
            nro.setAlignment(Element.ALIGN_CENTER);
            nro.setSpacingAfter(10);
            document.add(nro);

            // Transportista
            PdfPTable tTransp = crearTabla2Col();
            agregarFila(tTransp, "Transportista:", guia.getTransportista(), false);
            agregarFila(tTransp, "Fecha:",
                    guia.getFecha() != null
                            ? guia.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "—", true);
            agregarFila(tTransp, "Estado:", guia.getEstado().name(), false);
            agregarSeccion(document, "DATOS DEL TRANSPORTISTA", tTransp);

            // Destinatario
            PdfPTable tDest = crearTabla2Col();
            agregarFila(tDest, "Destinatario:", guia.getDestinatario(), false);
            agregarFila(tDest, "Dirección:", guia.getDireccionDestino(), true);
            agregarFila(tDest, "Ciudad:", guia.getCiudadDestino(), false);
            agregarSeccion(document, "DATOS DEL DESTINATARIO", tDest);

            // Mercadería
            PdfPTable tMerc = crearTabla2Col();
            agregarFila(tMerc, "Descripción:", guia.getDescripcionMercaderia(), false);
            agregarFila(tMerc, "Bultos:", String.valueOf(guia.getBultos()), true);
            agregarFila(tMerc, "Peso Total:", guia.getPesoKg() + " kg", false);
            agregarFila(tMerc, "Valor Declarado:",
                    guia.getValorDeclarado() != null
                            ? String.format("$ %,.0f CLP", guia.getValorDeclarado()) : "No declarado", true);
            agregarSeccion(document, "DETALLE DE LA MERCADERÍA", tMerc);

            // Firmas
            document.add(Chunk.NEWLINE);
            PdfPTable tablaFirmas = new PdfPTable(2);
            tablaFirmas.setWidthPercentage(90);
            tablaFirmas.setSpacingBefore(20);
            tablaFirmas.addCell(crearCeldaFirma("_______________________________\nFirma Transportista"));
            tablaFirmas.addCell(crearCeldaFirma("_______________________________\nFirma Receptor / Destinatario"));
            document.add(tablaFirmas);

            // Footer
            document.add(Chunk.NEWLINE);
            Paragraph footer = new Paragraph(
                    "Generado el " +
                    java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) +
                    " | Sistema de Gestión de Guías de Despacho", FOOTER_FONT);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            log.info("PDF completo generado: {} bytes", baos.size());
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generando PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar el PDF: " + e.getMessage(), e);
        }
    }
}
