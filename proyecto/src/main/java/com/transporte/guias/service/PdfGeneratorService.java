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

    private static final Font TITLE_FONT  = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.DARK_GRAY);
    private static final Font HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.WHITE);
    private static final Font LABEL_FONT  = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
    private static final Font VALUE_FONT  = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
    private static final Font FOOTER_FONT = new Font(Font.FontFamily.HELVETICA,  8, Font.ITALIC, BaseColor.GRAY);

    private static final BaseColor HEADER_BG = new BaseColor(46, 117, 182);
    private static final BaseColor ROW_ALT   = new BaseColor(235, 242, 250);

    /**
     * Genera el PDF completo de una guía de despacho.
     *
     * @param guia objeto con todos los datos del despacho
     * @return bytes del PDF generado
     */
    public byte[] generarPdfCompleto(Guia guia) {
        log.info("Generando PDF para guía ID: {}", guia.getId());
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

            document.add(Chunk.NEWLINE);

            // Sección Transportista
            PdfPTable tTransp = crearTabla2Col();
            agregarFila(tTransp, "Transportista:", guia.getTransportista(), false);
            agregarFila(tTransp, "Fecha:",
                    guia.getFecha() != null
                            ? guia.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "-", true);
            agregarFila(tTransp, "Estado:", guia.getEstado().name(), false);
            agregarSeccion(document, "DATOS DEL TRANSPORTISTA", tTransp);

            // Sección Destinatario
            PdfPTable tDest = crearTabla2Col();
            agregarFila(tDest, "Destinatario:", guia.getDestinatario(), false);
            agregarFila(tDest, "Dirección:", guia.getDireccionDestino(), true);
            agregarFila(tDest, "Ciudad:", guia.getCiudadDestino(), false);
            agregarSeccion(document, "DATOS DEL DESTINATARIO", tDest);

            // Sección Mercadería
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
            log.info("PDF generado: {} bytes", baos.size());
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generando PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar el PDF: " + e.getMessage(), e);
        }
    }

    // ── Métodos auxiliares ───────────────────────────────────────────────────

    private void agregarSeccion(Document doc, String titulo, PdfPTable tabla) throws DocumentException {
        PdfPTable header = new PdfPTable(1);
        header.setWidthPercentage(100);
        header.setSpacingBefore(14);
        header.setSpacingAfter(2);
        PdfPCell cell = new PdfPCell(new Phrase(titulo, HEADER_FONT));
        cell.setBackgroundColor(HEADER_BG);
        cell.setPadding(6);
        cell.setBorder(Rectangle.NO_BORDER);
        header.addCell(cell);
        doc.add(header);
        doc.add(tabla);
    }

    private PdfPTable crearTabla2Col() {
        PdfPTable tabla = new PdfPTable(new float[]{35f, 65f});
        tabla.setWidthPercentage(100);
        tabla.setSpacingAfter(6);
        return tabla;
    }

    private void agregarFila(PdfPTable tabla, String label, String valor, boolean altRow) {
        BaseColor bg = altRow ? ROW_ALT : BaseColor.WHITE;

        PdfPCell cLabel = new PdfPCell(new Phrase(label, LABEL_FONT));
        cLabel.setBackgroundColor(bg);
        cLabel.setPadding(6);
        cLabel.setBorderColor(new BaseColor(200, 200, 200));

        PdfPCell cValue = new PdfPCell(new Phrase(valor != null ? valor : "-", VALUE_FONT));
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
}
