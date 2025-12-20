package com.techforge.desktop;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.net.URL;

/**
 * UI Utilities - Helper class for custom Swing graphics.
 * Ported from the Web prototype CSS design system.
 *
 * Provides methods for:
 * - Custom drawn icons (no font dependencies - fixes square icons on Windows)
 * - Circular images with borders
 * - Card panels with drop shadows (CSS box-shadow)
 * - Status badges (CSS .status-planning, .status-active)
 * - Rounded buttons
 */
public class UIUtils {

    // ============================================
    // CUSTOM DRAWN ICONS (Fix for Square Icons on Windows)
    // ============================================

    /**
     * Creates a Dragon/Flame icon drawn with Graphics2D.
     * Used for logo and branding elements.
     *
     * @param size Icon size in pixels
     * @return Icon with dragon/flame shape in orange
     */
    public static Icon getDragonIcon(int size) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.translate(x, y);

                // Flame/Dragon shape gradient
                GradientPaint gradient = new GradientPaint(
                        0, 0, new Color(0xF8, 0x5B, 0x1A),
                        size, size, new Color(0xFF, 0x8C, 0x00)
                );
                g2.setPaint(gradient);

                // Draw flame shape
                Path2D flame = new Path2D.Float();
                float s = size / 24f; // Scale factor

                // Main flame body
                flame.moveTo(12 * s, 2 * s);
                flame.curveTo(14 * s, 6 * s, 18 * s, 8 * s, 18 * s, 14 * s);
                flame.curveTo(18 * s, 18 * s, 15 * s, 22 * s, 12 * s, 22 * s);
                flame.curveTo(9 * s, 22 * s, 6 * s, 18 * s, 6 * s, 14 * s);
                flame.curveTo(6 * s, 8 * s, 10 * s, 6 * s, 12 * s, 2 * s);
                flame.closePath();

                g2.fill(flame);

                // Inner flame highlight
                g2.setColor(new Color(0xFF, 0xDD, 0x00, 180));
                Path2D innerFlame = new Path2D.Float();
                innerFlame.moveTo(12 * s, 8 * s);
                innerFlame.curveTo(14 * s, 11 * s, 15 * s, 13 * s, 14 * s, 16 * s);
                innerFlame.curveTo(13 * s, 18 * s, 11 * s, 18 * s, 10 * s, 16 * s);
                innerFlame.curveTo(9 * s, 13 * s, 10 * s, 11 * s, 12 * s, 8 * s);
                innerFlame.closePath();
                g2.fill(innerFlame);

                g2.dispose();
            }

            @Override
            public int getIconWidth() { return size; }

            @Override
            public int getIconHeight() { return size; }
        };
    }

    /**
     * Creates a Scouter/Scanner icon drawn with Graphics2D.
     * Used for AI scanning features.
     *
     * @param size Icon size in pixels
     * @return Icon with scouter shape in blue
     */
    public static Icon getScouterIcon(int size) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.translate(x, y);

                float s = size / 24f;
                Color blue = new Color(0x07, 0x20, 0x83);
                Color lightBlue = new Color(0x3B, 0x82, 0xF6);

                // Outer frame (like a viewfinder)
                g2.setColor(blue);
                g2.setStroke(new BasicStroke(2 * s));
                g2.draw(new RoundRectangle2D.Float(3 * s, 3 * s, 18 * s, 18 * s, 4 * s, 4 * s));

                // Corner brackets
                g2.setStroke(new BasicStroke(2.5f * s));
                // Top-left
                g2.drawLine((int)(3 * s), (int)(7 * s), (int)(3 * s), (int)(3 * s));
                g2.drawLine((int)(3 * s), (int)(3 * s), (int)(7 * s), (int)(3 * s));
                // Top-right
                g2.drawLine((int)(17 * s), (int)(3 * s), (int)(21 * s), (int)(3 * s));
                g2.drawLine((int)(21 * s), (int)(3 * s), (int)(21 * s), (int)(7 * s));
                // Bottom-left
                g2.drawLine((int)(3 * s), (int)(17 * s), (int)(3 * s), (int)(21 * s));
                g2.drawLine((int)(3 * s), (int)(21 * s), (int)(7 * s), (int)(21 * s));
                // Bottom-right
                g2.drawLine((int)(17 * s), (int)(21 * s), (int)(21 * s), (int)(21 * s));
                g2.drawLine((int)(21 * s), (int)(17 * s), (int)(21 * s), (int)(21 * s));

                // Center target circle
                g2.setColor(lightBlue);
                g2.setStroke(new BasicStroke(1.5f * s));
                g2.draw(new Ellipse2D.Float(8 * s, 8 * s, 8 * s, 8 * s));

                // Center dot
                g2.fill(new Ellipse2D.Float(11 * s, 11 * s, 2 * s, 2 * s));

                g2.dispose();
            }

            @Override
            public int getIconWidth() { return size; }

            @Override
            public int getIconHeight() { return size; }
        };
    }

    /**
     * Creates a Checkmark icon drawn with Graphics2D.
     *
     * @param size Icon size in pixels
     * @return Icon with green checkmark
     */
    public static Icon getCheckIcon(int size) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.translate(x, y);

                float s = size / 24f;
                Color green = new Color(0x10, 0xB9, 0x81);

                // Circle background
                g2.setColor(green);
                g2.fill(new Ellipse2D.Float(2 * s, 2 * s, 20 * s, 20 * s));

                // White checkmark
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2.5f * s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                Path2D check = new Path2D.Float();
                check.moveTo(7 * s, 12 * s);
                check.lineTo(10 * s, 15 * s);
                check.lineTo(17 * s, 8 * s);
                g2.draw(check);

                g2.dispose();
            }

            @Override
            public int getIconWidth() { return size; }

            @Override
            public int getIconHeight() { return size; }
        };
    }

    /**
     * Creates a Plus icon drawn with Graphics2D.
     *
     * @param size Icon size in pixels
     * @return Icon with plus sign
     */
    public static Icon getPlusIcon(int size) {
        return getPlusIcon(size, AppTheme.SUCCESS);
    }

    /**
     * Creates a Plus icon with custom color.
     *
     * @param size Icon size in pixels
     * @param color Icon color
     * @return Icon with plus sign
     */
    public static Icon getPlusIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.translate(x, y);

                float s = size / 24f;

                // Circle background
                g2.setColor(color);
                g2.fill(new Ellipse2D.Float(2 * s, 2 * s, 20 * s, 20 * s));

                // White plus sign
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2.5f * s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                // Horizontal line
                g2.drawLine((int)(8 * s), (int)(12 * s), (int)(16 * s), (int)(12 * s));
                // Vertical line
                g2.drawLine((int)(12 * s), (int)(8 * s), (int)(12 * s), (int)(16 * s));

                g2.dispose();
            }

            @Override
            public int getIconWidth() { return size; }

            @Override
            public int getIconHeight() { return size; }
        };
    }

    /**
     * Creates a Refresh/Sync icon drawn with Graphics2D.
     *
     * @param size Icon size in pixels
     * @return Icon with circular arrows
     */
    public static Icon getRefreshIcon(int size) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.translate(x, y);

                float s = size / 24f;
                Color blue = new Color(0x3B, 0x82, 0xF6);

                g2.setColor(blue);
                g2.setStroke(new BasicStroke(2.5f * s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                // Arc (circular arrow)
                g2.draw(new Arc2D.Float(4 * s, 4 * s, 16 * s, 16 * s, 45, 270, Arc2D.OPEN));

                // Arrow head
                Path2D arrow = new Path2D.Float();
                arrow.moveTo(18 * s, 8 * s);
                arrow.lineTo(20 * s, 4 * s);
                arrow.lineTo(22 * s, 8 * s);
                g2.draw(arrow);

                g2.dispose();
            }

            @Override
            public int getIconWidth() { return size; }

            @Override
            public int getIconHeight() { return size; }
        };
    }

    /**
     * Creates a Delete/Trash icon drawn with Graphics2D.
     *
     * @param size Icon size in pixels
     * @return Icon with trash can shape
     */
    public static Icon getDeleteIcon(int size) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.translate(x, y);

                float s = size / 24f;
                Color red = new Color(0xDC, 0x26, 0x26);

                g2.setColor(red);
                g2.setStroke(new BasicStroke(2 * s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                // Lid
                g2.drawLine((int)(5 * s), (int)(7 * s), (int)(19 * s), (int)(7 * s));
                // Handle
                g2.draw(new RoundRectangle2D.Float(9 * s, 4 * s, 6 * s, 3 * s, 2 * s, 2 * s));

                // Body
                Path2D body = new Path2D.Float();
                body.moveTo(6 * s, 7 * s);
                body.lineTo(7 * s, 20 * s);
                body.lineTo(17 * s, 20 * s);
                body.lineTo(18 * s, 7 * s);
                g2.draw(body);

                // Lines inside
                g2.drawLine((int)(10 * s), (int)(10 * s), (int)(10 * s), (int)(17 * s));
                g2.drawLine((int)(14 * s), (int)(10 * s), (int)(14 * s), (int)(17 * s));

                g2.dispose();
            }

            @Override
            public int getIconWidth() { return size; }

            @Override
            public int getIconHeight() { return size; }
        };
    }

    /**
     * Creates an Edit/Pencil icon drawn with Graphics2D.
     *
     * @param size Icon size in pixels
     * @return Icon with pencil shape
     */
    public static Icon getEditIcon(int size) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.translate(x, y);

                float s = size / 24f;
                Color blue = new Color(0x3B, 0x82, 0xF6);

                g2.setColor(blue);
                g2.setStroke(new BasicStroke(2 * s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                // Pencil body (rotated rectangle)
                AffineTransform old = g2.getTransform();
                g2.rotate(Math.toRadians(-45), 12 * s, 12 * s);
                g2.draw(new RoundRectangle2D.Float(9 * s, 3 * s, 6 * s, 16 * s, 2 * s, 2 * s));
                g2.setTransform(old);

                // Tip
                g2.rotate(Math.toRadians(-45), 12 * s, 12 * s);
                Path2D tip = new Path2D.Float();
                tip.moveTo(9 * s, 19 * s);
                tip.lineTo(12 * s, 22 * s);
                tip.lineTo(15 * s, 19 * s);
                g2.draw(tip);
                g2.setTransform(old);

                g2.dispose();
            }

            @Override
            public int getIconWidth() { return size; }

            @Override
            public int getIconHeight() { return size; }
        };
    }

    /**
     * Creates a Folder icon for Project Planning.
     *
     * @param size Icon size in pixels
     * @return Icon with folder shape
     */
    public static Icon getFolderIcon(int size) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.translate(x, y);

                float s = size / 24f;
                Color blue = new Color(0x3B, 0x82, 0xF6);

                // Folder tab
                g2.setColor(blue);
                Path2D tab = new Path2D.Float();
                tab.moveTo(4 * s, 7 * s);
                tab.lineTo(4 * s, 5 * s);
                tab.lineTo(10 * s, 5 * s);
                tab.lineTo(11 * s, 7 * s);
                tab.closePath();
                g2.fill(tab);

                // Folder body
                g2.fill(new RoundRectangle2D.Float(4 * s, 7 * s, 16 * s, 12 * s, 2 * s, 2 * s));

                g2.dispose();
            }

            @Override
            public int getIconWidth() { return size; }

            @Override
            public int getIconHeight() { return size; }
        };
    }

    /**
     * Creates a Code/Brackets icon for Execution.
     *
     * @param size Icon size in pixels
     * @return Icon with code brackets
     */
    public static Icon getCodeIcon(int size) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.translate(x, y);

                float s = size / 24f;
                Color green = new Color(0x10, 0xB9, 0x81);

                g2.setColor(green);
                g2.setStroke(new BasicStroke(2 * s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                // Left bracket <
                Path2D leftBracket = new Path2D.Float();
                leftBracket.moveTo(10 * s, 7 * s);
                leftBracket.lineTo(6 * s, 12 * s);
                leftBracket.lineTo(10 * s, 17 * s);
                g2.draw(leftBracket);

                // Right bracket >
                Path2D rightBracket = new Path2D.Float();
                rightBracket.moveTo(14 * s, 7 * s);
                rightBracket.lineTo(18 * s, 12 * s);
                rightBracket.lineTo(14 * s, 17 * s);
                g2.draw(rightBracket);

                g2.dispose();
            }

            @Override
            public int getIconWidth() { return size; }

            @Override
            public int getIconHeight() { return size; }
        };
    }

    /**
     * Creates a Money/Dollar icon for Payroll.
     *
     * @param size Icon size in pixels
     * @return Icon with dollar sign
     */
    public static Icon getMoneyIcon(int size) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.translate(x, y);

                float s = size / 24f;
                Color gold = new Color(0xF5, 0x9E, 0x0B);

                // Circle background
                g2.setColor(new Color(gold.getRed(), gold.getGreen(), gold.getBlue(), 50));
                g2.fill(new Ellipse2D.Float(3 * s, 3 * s, 18 * s, 18 * s));

                // Dollar sign
                g2.setColor(gold);
                g2.setStroke(new BasicStroke(2 * s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                // Vertical line
                g2.drawLine((int)(12 * s), (int)(6 * s), (int)(12 * s), (int)(18 * s));

                // S shape
                Path2D sShape = new Path2D.Float();
                sShape.moveTo(15 * s, 8 * s);
                sShape.curveTo(15 * s, 7 * s, 13 * s, 6 * s, 11 * s, 7 * s);
                sShape.curveTo(9 * s, 8 * s, 9 * s, 10 * s, 11 * s, 11 * s);
                sShape.curveTo(13 * s, 12 * s, 13 * s, 14 * s, 11 * s, 15 * s);
                sShape.curveTo(9 * s, 16 * s, 7 * s, 15 * s, 7 * s, 14 * s);
                g2.draw(sShape);

                g2.dispose();
            }

            @Override
            public int getIconWidth() { return size; }

            @Override
            public int getIconHeight() { return size; }
        };
    }

    /**
     * Creates a Chart/Graph icon for Monitoring.
     *
     * @param size Icon size in pixels
     * @return Icon with bar chart
     */
    public static Icon getChartIcon(int size) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.translate(x, y);

                float s = size / 24f;
                Color purple = new Color(0x8B, 0x5C, 0xF6);

                g2.setColor(purple);
                g2.setStroke(new BasicStroke(2 * s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                // Bar 1 (short)
                g2.fillRoundRect((int)(5 * s), (int)(15 * s), (int)(3 * s), (int)(4 * s), (int)(1 * s), (int)(1 * s));

                // Bar 2 (medium)
                g2.fillRoundRect((int)(10 * s), (int)(12 * s), (int)(3 * s), (int)(7 * s), (int)(1 * s), (int)(1 * s));

                // Bar 3 (tall)
                g2.fillRoundRect((int)(15 * s), (int)(8 * s), (int)(3 * s), (int)(11 * s), (int)(1 * s), (int)(1 * s));

                // Trend line
                g2.setColor(new Color(0xF8, 0x5B, 0x1A));
                Path2D trendLine = new Path2D.Float();
                trendLine.moveTo(4 * s, 17 * s);
                trendLine.lineTo(11 * s, 14 * s);
                trendLine.lineTo(18 * s, 10 * s);
                g2.draw(trendLine);

                g2.dispose();
            }

            @Override
            public int getIconWidth() { return size; }

            @Override
            public int getIconHeight() { return size; }
        };
    }

    // ============================================
    // CARD PANEL - Matches CSS .card with box-shadow
    // ============================================

    /**
     * Creates a card panel with white background, rounded corners, and drop shadow.
     * Matches CSS: .card { background: white; border-radius: 12px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); }
     *
     * @return JPanel with card styling
     */
    public static JPanel createCardPanel() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int shadowOffset = 4;
                int cornerRadius = 16; // Updated for softer corners

                // Draw drop shadow (CSS box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1))
                g2.setColor(new Color(0, 0, 0, 25)); // ~10% opacity
                g2.fill(new RoundRectangle2D.Float(
                        shadowOffset, shadowOffset,
                        getWidth() - shadowOffset, getHeight() - shadowOffset,
                        cornerRadius, cornerRadius));

                // Draw white card background
                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Float(
                        0, 0,
                        getWidth() - shadowOffset, getHeight() - shadowOffset,
                        cornerRadius, cornerRadius));

                // Draw subtle border
                g2.setColor(AppTheme.BORDER);
                g2.setStroke(new BasicStroke(1));
                g2.draw(new RoundRectangle2D.Float(
                        0.5f, 0.5f,
                        getWidth() - shadowOffset - 1, getHeight() - shadowOffset - 1,
                        cornerRadius, cornerRadius));

                g2.dispose();
            }
        };

        card.setOpaque(false);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 24, 24)); // Extra padding for shadow

        return card;
    }

    /**
     * Creates a card panel with custom padding.
     *
     * @param padding Internal padding in pixels
     * @return JPanel with card styling
     */
    public static JPanel createCardPanel(int padding) {
        JPanel card = createCardPanel();
        card.setBorder(BorderFactory.createEmptyBorder(padding, padding, padding + 4, padding + 4));
        return card;
    }

    // ============================================
    // STATUS BADGE - Matches CSS .status-planning, .status-active
    // ============================================

    /**
     * Creates a status badge label with colors matching CSS.
     *
     * CSS Reference:
     * - .status-planning: background #FEF9C3, color #A16207 (Yellow/Amber)
     * - .status-active: background #DCFCE7, color #16A34A (Green)
     * - .status-doing: background #FFEDD5, color #EA580C (Orange)
     *
     * @param status Status string (TODO, DOING, DONE, PLANNING, ACTIVE, etc.)
     * @return JLabel styled as a badge
     */
    public static JLabel createStatusBadge(String status) {
        String displayText = status != null ? status.toUpperCase() : "UNKNOWN";

        JLabel badge = new JLabel(displayText) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Draw rounded background
                g2.setColor(getBackground());
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));

                g2.dispose();
                super.paintComponent(g);
            }
        };

        badge.setOpaque(false);
        badge.setHorizontalAlignment(SwingConstants.CENTER);
        badge.setFont(AppTheme.fontMain(Font.BOLD, 11));
        badge.setForeground(AppTheme.getStatusForeground(status));
        badge.setBackground(AppTheme.getStatusBackground(status));
        badge.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));

        return badge;
    }

    /**
     * Creates a priority badge with appropriate color.
     *
     * @param priority Priority string (HIGH, MEDIUM, LOW)
     * @return JLabel styled as a priority badge
     */
    public static JLabel createPriorityBadge(String priority) {
        String displayText = priority != null ? priority.toUpperCase() : "NORMAL";
        Color priorityColor = AppTheme.getPriorityColor(priority);

        JLabel badge = new JLabel(displayText) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Draw rounded background with 20% opacity of priority color
                g2.setColor(new Color(priorityColor.getRed(), priorityColor.getGreen(), priorityColor.getBlue(), 50));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));

                g2.dispose();
                super.paintComponent(g);
            }
        };

        badge.setOpaque(false);
        badge.setHorizontalAlignment(SwingConstants.CENTER);
        badge.setFont(AppTheme.fontMain(Font.BOLD, 10));
        badge.setForeground(priorityColor);
        badge.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        return badge;
    }

    /**
     * Creates a role badge (for profile display).
     *
     * @param role User role (MANAGER, EMPLOYEE, ADMIN, CLIENT)
     * @return JLabel styled as a role pill badge
     */
    public static JLabel createRoleBadge(String role) {
        Color badgeColor = getRoleBadgeColor(role);

        JLabel badge = new JLabel(role != null ? role : "USER") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Draw pill background
                g2.setColor(badgeColor);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));

                g2.dispose();
                super.paintComponent(g);
            }
        };

        badge.setOpaque(false);
        badge.setHorizontalAlignment(SwingConstants.CENTER);
        badge.setFont(AppTheme.fontMain(Font.BOLD, 11));
        badge.setForeground(Color.WHITE);
        badge.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));

        return badge;
    }

    /**
     * Get role badge color.
     */
    public static Color getRoleBadgeColor(String role) {
        if (role == null) return AppTheme.TEXT_LIGHT;
        switch (role.toUpperCase()) {
            case "MANAGER":
                return new Color(220, 38, 38);   // Red
            case "ADMIN":
                return new Color(147, 51, 234);  // Purple
            case "CLIENT":
                return new Color(16, 185, 129);  // Green
            case "EMPLOYEE":
            default:
                return new Color(59, 130, 246);  // Blue
        }
    }

    // ============================================
    // SKILL TAG - Matches CSS .skill-tag
    // ============================================

    /**
     * Creates a skill tag with light blue background.
     * Matches CSS: background #EFF6FF, rounded corners.
     *
     * @param skillName Skill name
     * @param level Skill level (optional)
     * @return JPanel containing skill tag
     */
    public static JPanel createSkillTag(String skillName, String level) {
        Color tagBg = new Color(0xEF, 0xF6, 0xFF); // #EFF6FF - Light Blue

        JPanel tag = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(tagBg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.dispose();
            }
        };

        tag.setOpaque(false);
        tag.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        String text = level != null && !level.isEmpty() ? skillName + ": " + level : skillName;
        JLabel label = new JLabel(text);
        label.setFont(AppTheme.fontMain(Font.BOLD, 11));
        label.setForeground(AppTheme.SECONDARY);
        tag.add(label);

        return tag;
    }

    // ============================================
    // CIRCULAR IMAGES
    // ============================================

    /**
     * Creates a circular image with anti-aliasing and a colored border.
     *
     * @param path Path to the image resource (e.g., "/assets/goku.png")
     * @param diameter Diameter of the circular image
     * @return ImageIcon with circular cropped image and border
     */
    public static ImageIcon createCircleImage(String path, int diameter) {
        return createCircleImage(path, diameter, AppTheme.PRIMARY);
    }

    /**
     * Creates a circular image with custom border color.
     *
     * @param path Path to the image resource
     * @param diameter Diameter of the circular image
     * @param borderColor Color of the border
     * @return ImageIcon with circular cropped image
     */
    public static ImageIcon createCircleImage(String path, int diameter, Color borderColor) {
        try {
            // Load image from classpath
            URL imageUrl = UIUtils.class.getResource(path);
            if (imageUrl == null && !path.startsWith("/")) {
                imageUrl = UIUtils.class.getResource("/" + path);
            }

            if (imageUrl == null) {
                System.err.println("Image not found: " + path);
                return createPlaceholderCircle(diameter, borderColor);
            }

            // Load and scale original image
            ImageIcon originalIcon = new ImageIcon(imageUrl);
            Image originalImage = originalIcon.getImage();

            // Create BufferedImage for circular cropping
            BufferedImage circleImage = new BufferedImage(diameter, diameter, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = circleImage.createGraphics();

            // Enable anti-aliasing
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            // Create circular clip
            Ellipse2D.Double circle = new Ellipse2D.Double(2, 2, diameter - 4, diameter - 4);
            g2.setClip(circle);

            // Draw scaled image within the circle
            g2.drawImage(originalImage, 2, 2, diameter - 4, diameter - 4, null);

            // Reset clip to draw border
            g2.setClip(null);

            // Draw border around the circle
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(2.5f));
            g2.draw(new Ellipse2D.Double(1.25, 1.25, diameter - 2.5, diameter - 2.5));

            g2.dispose();
            return new ImageIcon(circleImage);

        } catch (Exception e) {
            System.err.println("Error creating circle image: " + e.getMessage());
            return createPlaceholderCircle(diameter, borderColor);
        }
    }

    /**
     * Creates a placeholder circular icon with a question mark.
     */
    private static ImageIcon createPlaceholderCircle(int diameter, Color borderColor) {
        BufferedImage img = new BufferedImage(diameter, diameter, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw filled circle background
        g2.setColor(AppTheme.SECONDARY);
        g2.fill(new Ellipse2D.Double(2, 2, diameter - 4, diameter - 4));

        // Draw border
        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(2.5f));
        g2.draw(new Ellipse2D.Double(1.25, 1.25, diameter - 2.5, diameter - 2.5));

        // Draw initials or question mark
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, diameter / 3));
        FontMetrics fm = g2.getFontMetrics();
        String text = "?";
        int x = (diameter - fm.stringWidth(text)) / 2;
        int y = (diameter + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(text, x, y);

        g2.dispose();
        return new ImageIcon(img);
    }

    // ============================================
    // BUTTON UTILITIES
    // ============================================

    /**
     * Creates a rounded pill-shaped button with custom colors.
     */
    public static JButton createRoundedButton(String text, Color bg, Color fg) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color bgColor = bg;
                if (getModel().isPressed()) {
                    bgColor = bg.darker();
                } else if (getModel().isRollover()) {
                    bgColor = brighter(bg, 0.15f);
                } else if (!isEnabled()) {
                    bgColor = new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 100);
                }

                // Draw rounded rectangle background
                g2.setColor(bgColor);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));

                // Draw text
                g2.setColor(fg);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int textX = (getWidth() - fm.stringWidth(getText())) / 2;
                int textY = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), textX, textY);

                g2.dispose();
            }
        };

        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setForeground(fg);
        button.setFont(AppTheme.fontMain(Font.BOLD, 14));
        button.setPreferredSize(new Dimension(120, 40));

        return button;
    }

    /**
     * Creates a primary action button (orange background, white text).
     */
    public static JButton createPrimaryButton(String text) {
        return createRoundedButton(text, AppTheme.PRIMARY, Color.WHITE);
    }

    /**
     * Creates a secondary action button (navy background, white text).
     */
    public static JButton createSecondaryButton(String text) {
        return createRoundedButton(text, AppTheme.SECONDARY, Color.WHITE);
    }

    /**
     * Creates an outline button (transparent background, colored border).
     */
    public static JButton createOutlineButton(String text, Color color) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Hover fill
                if (getModel().isRollover()) {
                    g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 30));
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
                }

                // Draw border
                g2.setColor(color);
                g2.setStroke(new BasicStroke(2));
                g2.draw(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, getHeight() - 2, getHeight() - 2));

                // Draw text
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int textX = (getWidth() - fm.stringWidth(getText())) / 2;
                int textY = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), textX, textY);

                g2.dispose();
            }
        };

        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setForeground(color);
        button.setFont(AppTheme.fontMain(Font.BOLD, 14));
        button.setPreferredSize(new Dimension(120, 40));

        return button;
    }

    // ============================================
    // DIALOG UTILITIES
    // ============================================

    /**
     * Styles a JDialog with modern appearance.
     */
    public static void styleDialog(JDialog dialog) {
        dialog.getContentPane().setBackground(Color.WHITE);
        dialog.setLayout(new BorderLayout());
        dialog.setModal(true);
        dialog.setLocationRelativeTo(null);
    }

    /**
     * Creates a scrollable panel for dialog content.
     */
    public static JScrollPane createScrollableContent(JPanel content) {
        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return scrollPane;
    }

    // ============================================
    // BORDER UTILITIES
    // ============================================

    /**
     * Creates a drop shadow border matching CSS box-shadow.
     */
    public static Border createDropShadowBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 4, 4, new Color(0, 0, 0, 25)),
                BorderFactory.createLineBorder(AppTheme.BORDER, 1, true)
        );
    }

    /**
     * Creates a rounded border.
     */
    public static Border createRoundedBorder(int radius, Color color) {
        return BorderFactory.createLineBorder(color, 1, true);
    }

    // ============================================
    // COLOR UTILITIES
    // ============================================

    /**
     * Brightens a color by a given factor.
     */
    private static Color brighter(Color color, float factor) {
        int r = Math.min(255, (int) (color.getRed() + 255 * factor));
        int g = Math.min(255, (int) (color.getGreen() + 255 * factor));
        int b = Math.min(255, (int) (color.getBlue() + 255 * factor));
        return new Color(r, g, b, color.getAlpha());
    }
}

