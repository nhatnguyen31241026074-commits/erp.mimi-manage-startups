package com.techforge.desktop;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

/**
 * Application Theme - Global styling constants and utilities.
 * Ported from the Web prototype CSS design system.
 *
 * Reference CSS:
 * - Primary: #F85B1A (Orange)
 * - Secondary: #072083 (Navy)
 * - Background: #F0F2F5
 * - Font: 'Nunito' -> 'Segoe UI' in Java
 * - Shadows: box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1)
 */
public class AppTheme {

    // ============================================
    // COLOR PALETTE (Exact CSS Hex Values)
    // ============================================

    // Primary Colors (from CSS :root)
    public static final Color PRIMARY = new Color(0xF8, 0x5B, 0x1A);        // #F85B1A - Bright Orange
    public static final Color SECONDARY = new Color(0x07, 0x20, 0x83);      // #072083 - Navy Blue
    public static final Color ACCENT = new Color(0xF7, 0xB3, 0x2D);         // #F7B32D - Gold/Yellow

    // Status Colors
    public static final Color SUCCESS = new Color(0x10, 0xB9, 0x81);        // #10B981 - Emerald Green
    public static final Color DANGER = new Color(0xDC, 0x26, 0x26);         // #DC2626 - Red
    public static final Color WARNING = new Color(0xF5, 0x9E, 0x0B);        // #F59E0B - Amber

    // Text Colors
    public static final Color TEXT_MAIN = new Color(0x1A, 0x1A, 0x1A);      // #1A1A1A - Almost Black
    public static final Color TEXT_LIGHT = new Color(0x6B, 0x72, 0x80);     // #6B7280 - Medium Gray
    public static final Color TEXT_MUTED = new Color(0x9C, 0xA3, 0xAF);     // #9CA3AF - Light Gray

    // Background Colors (from CSS)
    public static final Color BACKGROUND = new Color(0xF0, 0xF2, 0xF5);     // #F0F2F5 - App Background
    public static final Color SURFACE = Color.WHITE;                         // #FFFFFF - Card Surface
    public static final Color BG_LIGHT = new Color(0xF9, 0xFA, 0xFB);       // #F9FAFB - Off White
    public static final Color BG_CARD = new Color(0xFF, 0xF5, 0xEB);        // #FFF5EB - Light Orange Tint
    public static final Color BORDER = new Color(0xE5, 0xE7, 0xEB);         // #E5E7EB - Border Gray

    // Sidebar Colors
    public static final Color SIDEBAR_BG = SECONDARY;                        // Navy sidebar
    public static final Color SIDEBAR_ACTIVE = PRIMARY;                      // Orange for active item
    public static final Color SIDEBAR_HOVER = new Color(255, 255, 255, 15); // Subtle white hover

    // Gradient Colors
    public static final Color GRADIENT_START = SECONDARY;
    public static final Color GRADIENT_END = new Color(0x03, 0x0A, 0x30);   // Darker navy

    // Kanban Colors
    public static final Color KANBAN_TODO = new Color(0x9C, 0xA3, 0xAF);    // #9CA3AF - Gray (Todo)
    public static final Color KANBAN_DOING = PRIMARY;                        // #F85B1A - Orange (Doing)
    public static final Color KANBAN_DONE = SUCCESS;                         // #10B981 - Green (Done)
    public static final Color KANBAN_BG = new Color(0xF3, 0xF4, 0xF6);      // #F3F4F6 - Light Gray

    // NEW: Dialog/UI specific colors
    public static final Color HEADER_BLUE = new Color(0x07, 0x20, 0x83);    // #072083 - Deep Navy for Dialog Headers
    public static final Color INPUT_BORDER = new Color(0xE5, 0xE7, 0xEB);   // #E5E7EB - Light Gray for inputs

    // ============================================
    // FONTS - CRITICAL: Use Segoe UI Emoji for icons
    // ============================================

    // Use Segoe UI Emoji as base to fix "Square Icons" on Windows
    private static final String FONT_EMOJI = "Segoe UI Emoji";
    private static final String FONT_FAMILY = "Segoe UI";
    private static final String FONT_FALLBACK = "Verdana";
    private static final String FONT_HEADING = "Segoe UI";

    /**
     * Returns main font with emoji support.
     * CRITICAL: Uses "Segoe UI Emoji" to properly render Unicode icons on Windows.
     */
    public static Font fontMain(int style, int size) {
        // Try Segoe UI Emoji first for icon support
        Font font = new Font(FONT_EMOJI, style, size);
        if (font.canDisplay('✅')) {
            return font;
        }
        // Fallback to regular Segoe UI
        font = new Font(FONT_FAMILY, style, size);
        if (!font.getFamily().equals(Font.DIALOG)) {
            return font;
        }
        // Last resort fallback
        return new Font(FONT_FALLBACK, style, size);
    }

    /**
     * Returns font specifically for emoji/icons.
     */
    public static Font fontEmoji(int size) {
        return new Font(FONT_EMOJI, Font.PLAIN, size);
    }

    /**
     * Returns heading font (bold).
     */
    public static Font fontHeading(int size) {
        return new Font(FONT_HEADING, Font.BOLD, size);
    }

    /**
     * Returns monospace font for code.
     */
    public static Font fontMono(int style, int size) {
        return new Font("Consolas", style, size);
    }

    // ============================================
    // DIMENSIONS (Match CSS values)
    // ============================================

    public static final int PADDING_SMALL = 10;
    public static final int PADDING_MEDIUM = 15;
    public static final int PADDING_LARGE = 25;
    public static final int BORDER_RADIUS = 16;        // Softer corners (was 12)
    public static final int BORDER_RADIUS_SMALL = 12;  // For inputs
    public static final int SIDEBAR_WIDTH = 280;

    // NEW: Global TextField/Dialog padding constants
    public static final int INPUT_PADDING_H = 15;      // Horizontal padding for inputs
    public static final int INPUT_PADDING_V = 12;      // Vertical padding for inputs
    public static final int DIALOG_PADDING = 25;       // Dialog content padding
    public static final int CARD_PADDING = 20;         // Card content padding

    public static final Dimension BUTTON_SIZE = new Dimension(200, 45);
    public static final Dimension INPUT_SIZE = new Dimension(320, 48);
    public static final Dimension LOGIN_CARD_SIZE = new Dimension(440, 580);

    // ============================================
    // SETUP METHOD - Apply FlatLaf Properties
    // ============================================

    /**
     * Apply FlatLaf properties to match CSS design system.
     * Call this in DesktopLauncher before creating any components.
     */
    public static void setup() {
        // ======== GLOBAL FONT FIX ========
        // Set modern Segoe UI 14px as default for professional look
        Font globalFont = new Font(FONT_FAMILY, Font.PLAIN, 14);
        Font globalFontBold = new Font(FONT_FAMILY, Font.BOLD, 14);

        // Apply global font to all components
        UIManager.put("defaultFont", globalFont);
        UIManager.put("Button.font", globalFontBold);
        UIManager.put("Label.font", globalFont);
        UIManager.put("TextField.font", globalFont);
        UIManager.put("PasswordField.font", globalFont);
        UIManager.put("TextArea.font", globalFont);
        UIManager.put("ComboBox.font", globalFont);
        UIManager.put("Table.font", globalFont);
        UIManager.put("TableHeader.font", globalFontBold);
        UIManager.put("TitledBorder.font", globalFontBold);
        UIManager.put("OptionPane.messageFont", globalFont);
        UIManager.put("OptionPane.buttonFont", globalFontBold);

        // Rounded corners - softer look
        UIManager.put("Button.arc", BORDER_RADIUS);           // 16 - Softer buttons
        UIManager.put("Component.arc", BORDER_RADIUS);        // 16 - Softer components
        UIManager.put("TextComponent.arc", BORDER_RADIUS_SMALL); // 12 - Inputs slightly less round
        UIManager.put("ProgressBar.arc", 10);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));

        // ======== GLOBAL PADDING FIX ========
        // TextField padding (10-15px as requested)
        UIManager.put("TextField.margin", new Insets(INPUT_PADDING_V, INPUT_PADDING_H, INPUT_PADDING_V, INPUT_PADDING_H));
        UIManager.put("PasswordField.margin", new Insets(INPUT_PADDING_V, INPUT_PADDING_H, INPUT_PADDING_V, INPUT_PADDING_H));
        UIManager.put("TextArea.margin", new Insets(PADDING_SMALL, PADDING_SMALL, PADDING_SMALL, PADDING_SMALL));
        UIManager.put("ComboBox.padding", new Insets(PADDING_SMALL, PADDING_MEDIUM, PADDING_SMALL, PADDING_MEDIUM));

        // Dialog padding
        UIManager.put("OptionPane.border", BorderFactory.createEmptyBorder(DIALOG_PADDING, DIALOG_PADDING, DIALOG_PADDING, DIALOG_PADDING));

        // Colors matching CSS
        UIManager.put("Button.default.background", PRIMARY);
        UIManager.put("Button.default.foreground", Color.WHITE);
        UIManager.put("Button.focusedBorderColor", PRIMARY);
        UIManager.put("Button.hoverBorderColor", PRIMARY.brighter());

        // Text field styling
        UIManager.put("TextField.background", SURFACE);
        UIManager.put("TextField.focusedBorderColor", PRIMARY);
        UIManager.put("TextField.borderColor", INPUT_BORDER);  // Use new INPUT_BORDER color
        UIManager.put("TextField.selectionBackground", new Color(PRIMARY.getRed(), PRIMARY.getGreen(), PRIMARY.getBlue(), 50));

        // Table styling
        UIManager.put("Table.selectionBackground", new Color(255, 245, 235));
        UIManager.put("Table.selectionForeground", TEXT_MAIN);
        UIManager.put("TableHeader.background", SURFACE);
        UIManager.put("TableHeader.foreground", SECONDARY);

        // Panel backgrounds
        UIManager.put("Panel.background", BACKGROUND);

        // ScrollBar styling
        UIManager.put("ScrollBar.track", BACKGROUND);
        UIManager.put("ScrollBar.thumb", BORDER);

        System.out.println("AppTheme.setup() - Design system applied successfully (Global fonts: Segoe UI 14px, Padding: 10-15px)");
    }

    // ============================================
    // BORDER UTILITIES
    // ============================================

    public static Border createRoundedBorder(int radius) {
        return BorderFactory.createEmptyBorder(radius, radius, radius, radius);
    }

    public static Border createLineBorder(Color color, int thickness) {
        return BorderFactory.createLineBorder(color, thickness, true);
    }

    public static Border createCardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        );
    }

    /**
     * Creates shadow border matching CSS: box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1)
     */
    public static Border createShadowBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 4, 4, new Color(0, 0, 0, 25)),
                BorderFactory.createLineBorder(BORDER, 1, true)
        );
    }

    // ============================================
    // BUTTON STYLING
    // ============================================

    public static void styleButton(JButton button, Color bgColor, Color fgColor) {
        button.setBackground(bgColor);
        button.setForeground(fgColor);
        button.setFont(fontMain(Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.putClientProperty("JButton.buttonType", "roundRect");
    }

    public static void stylePrimaryButton(JButton button) {
        styleButton(button, PRIMARY, Color.WHITE);
    }

    public static void styleSecondaryButton(JButton button) {
        styleButton(button, SECONDARY, Color.WHITE);
    }

    public static void styleOutlineButton(JButton button) {
        button.setBackground(Color.WHITE);
        button.setForeground(PRIMARY);
        button.setFont(fontMain(Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(PRIMARY, 2, true));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    // ============================================
    // INPUT STYLING
    // ============================================

    public static void styleTextField(JTextField field) {
        field.setFont(fontMain(Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        field.setBackground(Color.WHITE);
    }

    public static void styleTextFieldFocused(JTextField field) {
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PRIMARY, 2, true),
                BorderFactory.createEmptyBorder(11, 14, 11, 14)
        ));
    }

    // ============================================
    // TABLE STYLING
    // ============================================

    public static void styleTable(JTable table) {
        table.setRowHeight(50);
        table.setFont(fontMain(Font.PLAIN, 13));
        table.setSelectionBackground(new Color(255, 245, 235));
        table.setSelectionForeground(TEXT_MAIN);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);

        // Header styling matching CSS
        table.getTableHeader().setFont(fontMain(Font.BOLD, 12));
        table.getTableHeader().setBackground(Color.WHITE);
        table.getTableHeader().setForeground(SECONDARY);
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, BORDER));
        table.getTableHeader().setPreferredSize(new Dimension(0, 45));
    }

    // ============================================
    // STATUS BADGE COLORS (Match CSS .status-* classes)
    // ============================================

    /**
     * Get background color for status badge.
     * Matches CSS: .status-planning, .status-active, etc.
     */
    public static Color getStatusBackground(String status) {
        if (status == null) return new Color(243, 244, 246);
        switch (status.toUpperCase()) {
            case "ACTIVE":
            case "RUNNING":
            case "DONE":
            case "COMPLETED":
                return new Color(220, 252, 231); // #DCFCE7 - Light Green (CSS .status-active)
            case "PLANNING":
            case "TODO":
                return new Color(254, 249, 195); // #FEF9C3 - Light Yellow (CSS .status-planning)
            case "DOING":
            case "IN_PROGRESS":
                return new Color(255, 237, 213); // #FFEDD5 - Light Orange
            case "PENDING":
            case "PAUSED":
                return new Color(254, 226, 226); // #FEE2E2 - Light Red
            default:
                return new Color(243, 244, 246); // #F3F4F6 - Light Gray
        }
    }

    /**
     * Get foreground color for status badge.
     */
    public static Color getStatusForeground(String status) {
        if (status == null) return TEXT_LIGHT;
        switch (status.toUpperCase()) {
            case "ACTIVE":
            case "RUNNING":
            case "DONE":
            case "COMPLETED":
                return new Color(22, 163, 74);   // #16A34A - Green
            case "PLANNING":
            case "TODO":
                return new Color(161, 98, 7);    // #A16207 - Amber
            case "DOING":
            case "IN_PROGRESS":
                return new Color(234, 88, 12);   // #EA580C - Orange
            case "PENDING":
            case "PAUSED":
                return new Color(220, 38, 38);   // #DC2626 - Red
            default:
                return TEXT_LIGHT;
        }
    }

    // ============================================
    // PRIORITY COLORS
    // ============================================

    public static Color getPriorityColor(String priority) {
        if (priority == null) return TEXT_LIGHT;
        switch (priority.toUpperCase()) {
            case "HIGH":
            case "URGENT":
                return DANGER;      // Red
            case "MEDIUM":
            case "MED":
                return WARNING;     // Amber
            case "LOW":
                return SUCCESS;     // Green
            default:
                return TEXT_LIGHT;
        }
    }

    // ============================================
    // SHADOW COLORS (CSS box-shadow simulation)
    // ============================================

    public static final Color SHADOW_LIGHT = new Color(0, 0, 0, 25);    // 10% opacity
    public static final Color SHADOW_MEDIUM = new Color(0, 0, 0, 40);   // 15% opacity
    public static final Color SHADOW_DARK = new Color(0, 0, 0, 60);     // 25% opacity
}

