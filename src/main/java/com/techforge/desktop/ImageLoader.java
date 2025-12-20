package com.techforge.desktop;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;

/**
 * Utility class for loading images and avatars.
 * Simple, robust implementation for classpath resource loading.
 */
public class ImageLoader {

    /**
     * Load avatar based on user role.
     * @param role User role (MANAGER, EMPLOYEE, ADMIN, CLIENT)
     * @return ImageIcon of the avatar, or a placeholder if not found
     */
    public static ImageIcon loadAvatar(String role) {
        String fileName;
        switch (role != null ? role.toUpperCase() : "") {
            case "MANAGER":
                fileName = "vegeta.png";
                break;
            case "EMPLOYEE":
                fileName = "goku.png";
                break;
            case "ADMIN":
                fileName = "bulma.png";
                break;
            case "CLIENT":
                fileName = "frieza.png";
                break;
            default:
                fileName = "goku.png";
                break;
        }

        ImageIcon icon = loadImage("/assets/" + fileName);
        if (icon != null && icon.getIconWidth() > 0) {
            // Scale to 50x50
            Image scaled = icon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        }
        return createPlaceholderIcon(50, 50);
    }

    /**
     * Load an image from classpath.
     * Simple, robust implementation.
     * @param path Path relative to resources (e.g., "/assets/goku.png")
     * @return ImageIcon or null if not found
     */
    public static ImageIcon loadImage(String path) {
        try {
            // Try loading from classpath
            URL url = ImageLoader.class.getResource(path);

            // If not found, try adding slash if missing
            if (url == null && !path.startsWith("/")) {
                url = ImageLoader.class.getResource("/" + path);
            }

            // If still not found, try alternative paths
            if (url == null) {
                String fileName = path.substring(path.lastIndexOf("/") + 1);
                String[] altPaths = {
                    "/static/assets/" + fileName,
                    "/assets/" + fileName,
                    "assets/" + fileName
                };
                for (String altPath : altPaths) {
                    url = ImageLoader.class.getResource(altPath);
                    if (url != null) break;
                }
            }

            if (url != null) {
                System.out.println("✓ Loaded image: " + url);
                return new ImageIcon(url);
            } else {
                System.err.println("✗ Image not found: " + path);
                return null;
            }
        } catch (Exception e) {
            System.err.println("✗ Error loading image: " + path);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Load and resize an image.
     */
    public static ImageIcon loadImage(String path, int width, int height) {
        ImageIcon icon = loadImage(path);
        if (icon != null && icon.getIconWidth() > 0) {
            Image scaled = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        }
        return createPlaceholderIcon(width, height);
    }

    /**
     * Create a circular placeholder icon with a "?" symbol.
     */
    public static ImageIcon createPlaceholderIcon(int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw circle background
        g2.setColor(AppTheme.PRIMARY);
        g2.fillOval(0, 0, width, height);

        // Draw border
        g2.setColor(AppTheme.ACCENT);
        g2.setStroke(new BasicStroke(2));
        g2.drawOval(1, 1, width - 3, height - 3);

        // Draw question mark
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, width / 2));
        FontMetrics fm = g2.getFontMetrics();
        String text = "?";
        int x = (width - fm.stringWidth(text)) / 2;
        int y = (height + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(text, x, y);

        g2.dispose();
        return new ImageIcon(img);
    }

    /**
     * Create a circular avatar with initials (fallback when no image).
     */
    public static ImageIcon createInitialsAvatar(String name, int size, Color bgColor) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw circle
        g2.setColor(bgColor);
        g2.fillOval(0, 0, size, size);

        // Draw initials
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, size / 2));
        String initials = name != null && !name.isEmpty()
                ? name.substring(0, 1).toUpperCase()
                : "U";
        FontMetrics fm = g2.getFontMetrics();
        int x = (size - fm.stringWidth(initials)) / 2;
        int y = (size + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(initials, x, y);

        g2.dispose();
        return new ImageIcon(img);
    }
}

