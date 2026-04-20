package com.adbmanager.view.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Icon;

public class ToolbarIcon implements Icon {

    private static final Pattern SVG_TOKEN = Pattern.compile(
            "[A-Za-z]|[-+]?(?:\\d*\\.\\d+|\\d+)(?:[eE][-+]?\\d+)?");
    private static final int VIEWBOX_SIZE = 24;

    public enum Type {
        HOME("M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"),
        DISPLAY("M17 1H7c-1.1 0-2 .9-2 2v18c0 1.1.9 2 2 2h10c1.1 0 2-.9 2-2V3c0-1.1-.9-2-2-2zm0 16H7V5h10v12z"),
        CONTROL("M3 17h6v2H3v-2zm0-6h10v2H3v-2zm0-6h14v2H3V5zm11 12h7v2h-7v-2zm4-6h3v2h-3v-2zm-6-6h9v2h-9V5z"),
        APPS("M4 8h4V4H4v4zm6 0h4V4h-4v4zm6 0h4V4h-4v4z M4 14h4v-4H4v4zm6 0h4v-4h-4v4zm6 0h4v-4h-4v4z M4 20h4v-4H4v4zm6 0h4v-4h-4v4zm6 0h4v-4h-4v4z"),
        FILES("M6 2h9l5 5v15H6z M14 2v6h6"),
        FOLDER("M10 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z"),
        WIRELESS("M12 18.5c.83 0 1.5.67 1.5 1.5S12.83 21.5 12 21.5 10.5 20.83 10.5 20 11.17 18.5 12 18.5z M7.05 14.05 8.46 15.46C9.39 14.53 10.64 14 12 14s2.61.53 3.54 1.46l1.41-1.41C15.66 12.76 13.89 12 12 12s-3.66.76-4.95 2.05z M2.81 9.81l1.41 1.41C6.3 9.14 9.05 8 12 8s5.7 1.14 7.78 3.22l1.41-1.41C18.75 7.36 15.5 6 12 6S5.25 7.36 2.81 9.81z M12 2C7.03 2 2.73 3.92 0 6.81l1.41 1.41C3.78 5.86 7.67 4 12 4s8.22 1.86 10.59 4.22L24 6.81C21.27 3.92 16.97 2 12 2z"),

        // SVG original:
        // <svg viewBox="0 -960 960 960"><path d="..."/></svg>
        SYSTEM(
            "M40-240q9-107 65.5-197T256-580l-74-128q-6-9-3-19t13-15q8-5 18-2t16 12l74 128q86-36 180-36t180 36l74-128q6-9 16-12t18 2q10 5 13 15t-3 19l-74 128q94 53 150.5 143T920-240H40Zm275.5-124.5Q330-379 330-400t-14.5-35.5Q301-450 280-450t-35.5 14.5Q230-421 230-400t14.5 35.5Q259-350 280-350t35.5-14.5Zm400 0Q730-379 730-400t-14.5-35.5Q701-450 680-450t-35.5 14.5Q630-421 630-400t14.5 35.5Q659-350 680-350t35.5-14.5Z",
            0, -960, 960, 960
        ),

        REFRESH("M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"),
        POWER("M11 2h2v9h-2z M8 7 9.42 8.42C7.93 9.56 7 11.37 7 13c0 3.31 2.69 6 6 6s6-2.69 6-6c0-1.63-.93-3.44-2.42-4.58L18 7c1.87 1.52 3 3.85 3 6 0 4.42-3.58 8-8 8s-8-3.58-8-8c0-2.15 1.13-4.48 3-6z"),
        SETTINGS("M19.14,12.94c0.04-0.31,0.06-0.63,0.06-0.94s-0.02-0.63-0.06-0.94l2.03-1.58c0.18-0.14,0.23-0.4,0.12-0.61l-1.92-3.32c-0.12-0.22-0.37-0.3-0.59-0.22l-2.39,0.96c-0.5-0.38-1.05-0.69-1.66-0.92L14.46,2.5C14.43,2.24,14.21,2,13.95,2h-3.9C9.79,2,9.57,2.24,9.54,2.5L9.17,5.03C8.56,5.26,8.01,5.58,7.51,5.95L5.12,4.99c-0.22-0.09-0.47,0-0.59,0.22L2.61,8.53c-0.12,0.22-0.07,0.47,0.12,0.61l2.03,1.58C4.72,11.37,4.7,11.69,4.7,12s0.02,0.63,0.06,0.94l-2.03,1.58c-0.18,0.14-0.23,0.4-0.12,0.61l1.92,3.32c0.12,0.22,0.37,0.3,0.59,0.22l2.39-0.96c0.5,0.38,1.05,0.69,1.66,0.92l0.37,2.53c0.03,0.26,0.25,0.5,0.51,0.5h3.9c0.26,0,0.48-0.24,0.51-0.5l0.37-2.53c0.61-0.23,1.16-0.55,1.66-0.92l2.39,0.96c0.22,0.09,0.47,0,0.59-0.22l1.92-3.32c0.12-0.22,0.07-0.47-0.12-0.61L19.14,12.94z M12,15.5c-1.93,0-3.5-1.57-3.5-3.5s1.57-3.5,3.5-3.5s3.5,1.57,3.5,3.5S13.93,15.5,12,15.5z"),
        OPEN("M14 3h7v7h-2V6.41l-9.29 9.3-1.42-1.42 9.3-9.29H14V3z M5 5h6v2H7v10h10v-4h2v6H5V5z"),
        STOP("M6 6h12v12H6z"),
        UNINSTALL("M7 7h10l-1 12H8L7 7z M9 4h6l1 2H8l1-2z"),
        ENABLE("M9 16.17 4.83 12l-1.42 1.41L9 19 20.59 7.41 19.17 6z"),
        DISABLE("M7 7h10v10H7z M4.22 4.22l15.56 15.56-1.41 1.41L2.81 5.63z"),
        CLEAR_DATA("M7 5h10l1 2H6l1-2z M6 8h12l-1 12H7L6 8z"),
        CLEAR_CACHE("M4 14h16v2H4z M7 10h10v2H7z M10 4h4v4h-4z"),
        EXPORT("M5 20h14v-2H5v2z M12 2l5.5 5.5h-4v7h-3v-7h-4L12 2z"),
        DOWNLOAD("M5 18h14v2H5z M11 6h2v6h3l-4 4-4-4h3z"),
        UPLOAD("M5 18h14v2H5z M11 16h2v-6h3l-4-4-4 4h3z"),
        EDIT("M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25z M20.71 7.04c.39-.39.39-1.02 0-1.41L18.37 3.29c-.39-.39-1.02-.39-1.41 0l-1.84 1.84 3.75 3.75 1.84-1.84z"),
        COPY("M16 1H4v14h2V3h10z M20 5H8v18h12z"),
        NEW_FOLDER("M3 6h5l2 2h11v12H3z M3 6v14h18V8H10L8 6H3z M15 11h-2v2h-2v2h2v2h2v-2h2v-2h-2z"),
        ARROW_UP("M12 4l7 8h-4v8h-6v-8H5z"),
        ARROW_DOWN("M12 20 5 12h4V4h6v8h4z"),
        ARROW_LEFT("M4 12l8-7v4h8v6h-8v4z"),
        ARROW_RIGHT("M20 12l-8 7v-4H4V9h8V5z"),
        ADD("M19 11h-6V5h-2v6H5v2h6v6h2v-6h6z");

        private final Shape shape;

        Type(String pathData) {
            this(pathData, 0, 0, VIEWBOX_SIZE, VIEWBOX_SIZE);
        }

        Type(String pathData, double viewBoxMinX, double viewBoxMinY, double viewBoxWidth, double viewBoxHeight) {
            Shape parsed = SvgPathParser.parse(pathData);

            if (viewBoxMinX != 0 || viewBoxMinY != 0 || viewBoxWidth != VIEWBOX_SIZE || viewBoxHeight != VIEWBOX_SIZE) {
                parsed = AffineTransform
                        .getTranslateInstance(-viewBoxMinX, -viewBoxMinY)
                        .createTransformedShape(parsed);

                parsed = AffineTransform
                        .getScaleInstance(VIEWBOX_SIZE / viewBoxWidth, VIEWBOX_SIZE / viewBoxHeight)
                        .createTransformedShape(parsed);
            }

            this.shape = parsed;
        }

        public Shape shape() {
            return shape;
        }
    }

    private final Type type;
    private final int size;
    private final Color color;

    public ToolbarIcon(Type type, int size, Color color) {
        this.type = type;
        this.size = size;
        this.color = color;
    }

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D g2d = (Graphics2D) graphics.create();
        g2d.translate(x, y);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2d.scale(size / (double) VIEWBOX_SIZE, size / (double) VIEWBOX_SIZE);
        g2d.setColor(color);
        g2d.fill(type.shape());
        g2d.dispose();
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }

    private static final class SvgPathParser {

        private SvgPathParser() {
        }

        public static Shape parse(String pathData) {
            List<String> tokens = tokenize(pathData);
            Path2D.Double path = new Path2D.Double(Path2D.WIND_NON_ZERO);

            double currentX = 0;
            double currentY = 0;
            double startX = 0;
            double startY = 0;

            double lastCubicControlX = 0;
            double lastCubicControlY = 0;
            double lastQuadControlX = 0;
            double lastQuadControlY = 0;

            char command = ' ';
            char previousCommand = ' ';
            int index = 0;

            while (index < tokens.size()) {
                String token = tokens.get(index);
                if (isCommand(token)) {
                    command = token.charAt(0);
                    index++;
                }

                switch (command) {
                    case 'M', 'm' -> {
                        boolean relative = command == 'm';

                        double x = nextNumber(tokens, index++);
                        double y = nextNumber(tokens, index++);
                        if (relative) {
                            x += currentX;
                            y += currentY;
                        }

                        path.moveTo(x, y);
                        currentX = x;
                        currentY = y;
                        startX = x;
                        startY = y;

                        lastCubicControlX = x;
                        lastCubicControlY = y;
                        lastQuadControlX = x;
                        lastQuadControlY = y;

                        while (hasNumber(tokens, index)) {
                            x = nextNumber(tokens, index++);
                            y = nextNumber(tokens, index++);
                            if (relative) {
                                x += currentX;
                                y += currentY;
                            }

                            path.lineTo(x, y);
                            currentX = x;
                            currentY = y;

                            lastCubicControlX = x;
                            lastCubicControlY = y;
                            lastQuadControlX = x;
                            lastQuadControlY = y;
                        }
                    }

                    case 'L', 'l' -> {
                        boolean relative = command == 'l';
                        while (hasNumber(tokens, index)) {
                            double x = nextNumber(tokens, index++);
                            double y = nextNumber(tokens, index++);
                            if (relative) {
                                x += currentX;
                                y += currentY;
                            }
                            path.lineTo(x, y);
                            currentX = x;
                            currentY = y;

                            lastCubicControlX = x;
                            lastCubicControlY = y;
                            lastQuadControlX = x;
                            lastQuadControlY = y;
                        }
                    }

                    case 'H', 'h' -> {
                        boolean relative = command == 'h';
                        while (hasNumber(tokens, index)) {
                            double x = nextNumber(tokens, index++);
                            if (relative) {
                                x += currentX;
                            }
                            path.lineTo(x, currentY);
                            currentX = x;

                            lastCubicControlX = currentX;
                            lastCubicControlY = currentY;
                            lastQuadControlX = currentX;
                            lastQuadControlY = currentY;
                        }
                    }

                    case 'V', 'v' -> {
                        boolean relative = command == 'v';
                        while (hasNumber(tokens, index)) {
                            double y = nextNumber(tokens, index++);
                            if (relative) {
                                y += currentY;
                            }
                            path.lineTo(currentX, y);
                            currentY = y;

                            lastCubicControlX = currentX;
                            lastCubicControlY = currentY;
                            lastQuadControlX = currentX;
                            lastQuadControlY = currentY;
                        }
                    }

                    case 'C', 'c' -> {
                        boolean relative = command == 'c';
                        while (hasNumber(tokens, index)) {
                            double x1 = nextNumber(tokens, index++);
                            double y1 = nextNumber(tokens, index++);
                            double x2 = nextNumber(tokens, index++);
                            double y2 = nextNumber(tokens, index++);
                            double x = nextNumber(tokens, index++);
                            double y = nextNumber(tokens, index++);

                            if (relative) {
                                x1 += currentX;
                                y1 += currentY;
                                x2 += currentX;
                                y2 += currentY;
                                x += currentX;
                                y += currentY;
                            }

                            path.curveTo(x1, y1, x2, y2, x, y);
                            currentX = x;
                            currentY = y;
                            lastCubicControlX = x2;
                            lastCubicControlY = y2;
                            lastQuadControlX = x;
                            lastQuadControlY = y;
                        }
                    }

                    case 'S', 's' -> {
                        boolean relative = command == 's';
                        while (hasNumber(tokens, index)) {
                            double reflectedX = switch (previousCommand) {
                                case 'C', 'c', 'S', 's' -> (2 * currentX) - lastCubicControlX;
                                default -> currentX;
                            };
                            double reflectedY = switch (previousCommand) {
                                case 'C', 'c', 'S', 's' -> (2 * currentY) - lastCubicControlY;
                                default -> currentY;
                            };

                            double x2 = nextNumber(tokens, index++);
                            double y2 = nextNumber(tokens, index++);
                            double x = nextNumber(tokens, index++);
                            double y = nextNumber(tokens, index++);

                            if (relative) {
                                x2 += currentX;
                                y2 += currentY;
                                x += currentX;
                                y += currentY;
                            }

                            path.curveTo(reflectedX, reflectedY, x2, y2, x, y);
                            currentX = x;
                            currentY = y;
                            lastCubicControlX = x2;
                            lastCubicControlY = y2;
                            lastQuadControlX = x;
                            lastQuadControlY = y;
                        }
                    }

                    case 'Q', 'q' -> {
                        boolean relative = command == 'q';
                        while (hasNumber(tokens, index)) {
                            double x1 = nextNumber(tokens, index++);
                            double y1 = nextNumber(tokens, index++);
                            double x = nextNumber(tokens, index++);
                            double y = nextNumber(tokens, index++);

                            if (relative) {
                                x1 += currentX;
                                y1 += currentY;
                                x += currentX;
                                y += currentY;
                            }

                            path.quadTo(x1, y1, x, y);
                            currentX = x;
                            currentY = y;
                            lastQuadControlX = x1;
                            lastQuadControlY = y1;
                            lastCubicControlX = x;
                            lastCubicControlY = y;
                        }
                    }

                    case 'T', 't' -> {
                        boolean relative = command == 't';
                        while (hasNumber(tokens, index)) {
                            double reflectedX = switch (previousCommand) {
                                case 'Q', 'q', 'T', 't' -> (2 * currentX) - lastQuadControlX;
                                default -> currentX;
                            };
                            double reflectedY = switch (previousCommand) {
                                case 'Q', 'q', 'T', 't' -> (2 * currentY) - lastQuadControlY;
                                default -> currentY;
                            };

                            double x = nextNumber(tokens, index++);
                            double y = nextNumber(tokens, index++);

                            if (relative) {
                                x += currentX;
                                y += currentY;
                            }

                            path.quadTo(reflectedX, reflectedY, x, y);
                            currentX = x;
                            currentY = y;
                            lastQuadControlX = reflectedX;
                            lastQuadControlY = reflectedY;
                            lastCubicControlX = x;
                            lastCubicControlY = y;
                        }
                    }

                    case 'Z', 'z' -> {
                        path.closePath();
                        currentX = startX;
                        currentY = startY;
                        lastCubicControlX = startX;
                        lastCubicControlY = startY;
                        lastQuadControlX = startX;
                        lastQuadControlY = startY;
                    }

                    default -> throw new IllegalArgumentException("Unsupported SVG path command: " + command);
                }

                previousCommand = command;
            }

            return path;
        }

        private static List<String> tokenize(String pathData) {
            List<String> tokens = new ArrayList<>();
            Matcher matcher = SVG_TOKEN.matcher(pathData);
            while (matcher.find()) {
                tokens.add(matcher.group());
            }
            return tokens;
        }

        private static boolean isCommand(String token) {
            return token.length() == 1 && Character.isLetter(token.charAt(0));
        }

        private static boolean hasNumber(List<String> tokens, int index) {
            return index < tokens.size() && !isCommand(tokens.get(index));
        }

        private static double nextNumber(List<String> tokens, int index) {
            return Double.parseDouble(tokens.get(index));
        }
    }
}
