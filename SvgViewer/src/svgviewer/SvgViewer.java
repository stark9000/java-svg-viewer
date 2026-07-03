package svgviewer;

import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.swing.gvt.GVTTreeRendererAdapter;
import org.apache.batik.swing.gvt.GVTTreeRendererEvent;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.File;

/**
 * Simple SVG viewer built on Apache Batik.
 *
 * Features: - File -> Open (SVG files only) - Pan (click + drag) - Zoom in /
 * out (buttons, menu, mouse wheel, keyboard) - Fit to screen (auto on load,
 * also manual) - Actual size (100%) - Remembers last folder opened, for the
 * lifetime of the running app only
 */
public class SvgViewer extends JFrame {

    private final JSVGCanvas canvas = new JSVGCanvas();
    private final JLabel statusLabel = new JLabel("No file open");

    private File lastDirectory = null;      // in-memory only, resets on restart
    private String currentFileName = "No file open";
    private double currentScale = 1.0;
    private Point lastDragPoint = null;
    private boolean pendingFitToScreen = false;

    private static final double ZOOM_STEP_IN = 1.25;
    private static final double ZOOM_STEP_OUT = 1.0 / 1.25;

    public SvgViewer() {
        super("SVG Viewer");
        initCanvas();
        initUI();
    }

    // ---------------------------------------------------------------
    // Setup
    // ---------------------------------------------------------------
    private void initCanvas() {
        canvas.setBackground(Color.WHITE);

        // Turn off Batik's built-in gesture interactors so our own
        // pan / zoom / wheel handlers are the only thing driving the view.
        canvas.setEnableImageZoomInteractor(false);
        canvas.setEnableZoomInteractor(false);
        canvas.setEnablePanInteractor(false);
        canvas.setEnableRotateInteractor(false);
        canvas.setEnableResetTransformInteractor(false);

        // Fit-to-screen once, right after a new document finishes rendering.
        canvas.addGVTTreeRendererListener(new GVTTreeRendererAdapter() {
            @Override
            public void gvtRenderingCompleted(GVTTreeRendererEvent e) {
                if (pendingFitToScreen) {
                    pendingFitToScreen = false;
                    SwingUtilities.invokeLater(() -> {
                        fitToScreen();
                        updateStatus();
                    });
                }
            }
        });

        // --- Pan: click + drag ---
        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastDragPoint = e.getPoint();
                canvas.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                lastDragPoint = null;
                canvas.setCursor(Cursor.getDefaultCursor());
            }
        });
        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (lastDragPoint == null) {
                    return;
                }
                int dx = e.getX() - lastDragPoint.x;
                int dy = e.getY() - lastDragPoint.y;
                translate(dx, dy);
                lastDragPoint = e.getPoint();
            }
        });

        // --- Zoom: mouse wheel, centered on cursor ---
        canvas.addMouseWheelListener(e -> {
            double factor = e.getWheelRotation() < 0 ? ZOOM_STEP_IN : ZOOM_STEP_OUT;
            zoomAtPoint(factor, e.getPoint());
        });
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 750);
        setLocationRelativeTo(null);

        setJMenuBar(buildMenuBar());
        add(buildToolBar(), BorderLayout.NORTH);
        add(canvas, BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem open = new JMenuItem("Open...");
        open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent_CTRL()));
        open.addActionListener(this::onOpen);
        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> System.exit(0));
        fileMenu.add(open);
        fileMenu.addSeparator();
        fileMenu.add(exit);

        JMenu viewMenu = new JMenu("View");
        JMenuItem zoomIn = new JMenuItem("Zoom In");
        zoomIn.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, InputEvent_CTRL()));
        zoomIn.addActionListener(e -> zoomAtCenter(ZOOM_STEP_IN));

        JMenuItem zoomOut = new JMenuItem("Zoom Out");
        zoomOut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent_CTRL()));
        zoomOut.addActionListener(e -> zoomAtCenter(ZOOM_STEP_OUT));

        JMenuItem fit = new JMenuItem("Fit to Screen");
        fit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent_CTRL()));
        fit.addActionListener(e -> {
            fitToScreen();
            updateStatus();
        });

        JMenuItem actual = new JMenuItem("Actual Size (100%)");
        actual.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, InputEvent_CTRL()));
        actual.addActionListener(e -> {
            actualSize();
            updateStatus();
        });

        viewMenu.add(zoomIn);
        viewMenu.add(zoomOut);
        viewMenu.addSeparator();
        viewMenu.add(fit);
        viewMenu.add(actual);

        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        return menuBar;
    }

    // Cross-platform-safe modifier (Ctrl on Win/Linux). Kept as a helper
    // so it's easy to swap for the platform menu mask if you package this
    // for macOS later.
    private int InputEvent_CTRL() {
        return java.awt.event.InputEvent.CTRL_DOWN_MASK;
    }

    private JToolBar buildToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton openBtn = new JButton("Open...");
        openBtn.addActionListener(this::onOpen);

        JButton zoomInBtn = new JButton("Zoom In");
        zoomInBtn.addActionListener(e -> zoomAtCenter(ZOOM_STEP_IN));

        JButton zoomOutBtn = new JButton("Zoom Out");
        zoomOutBtn.addActionListener(e -> zoomAtCenter(ZOOM_STEP_OUT));

        JButton fitBtn = new JButton("Fit to Screen");
        fitBtn.addActionListener(e -> {
            fitToScreen();
            updateStatus();
        });

        JButton actualBtn = new JButton("100%");
        actualBtn.addActionListener(e -> {
            actualSize();
            updateStatus();
        });

        toolBar.add(openBtn);
        toolBar.addSeparator();
        toolBar.add(zoomInBtn);
        toolBar.add(zoomOutBtn);
        toolBar.add(fitBtn);
        toolBar.add(actualBtn);
        return toolBar;
    }

    private JPanel buildStatusBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        panel.add(statusLabel, BorderLayout.WEST);
        return panel;
    }

    // ---------------------------------------------------------------
    // File open
    // ---------------------------------------------------------------
    private void onOpen(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        if (lastDirectory != null) {
            chooser.setCurrentDirectory(lastDirectory);
        }
        chooser.setFileFilter(new FileNameExtensionFilter("SVG files (*.svg)", "svg"));

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            lastDirectory = file.getParentFile(); // remembered only for this run
            loadFile(file);
        }
    }

    private void loadFile(File file) {
        currentFileName = file.getName();
        setTitle("SVG Viewer - " + currentFileName);
        pendingFitToScreen = true;
        canvas.setURI(file.toURI().toString());
        updateStatus();
    }

    // ---------------------------------------------------------------
    // Pan / zoom / fit
    // ---------------------------------------------------------------
    private void translate(int dx, int dy) {
        AffineTransform at = canvas.getRenderingTransform();
        if (at == null) {
            at = new AffineTransform();
        }
        AffineTransform newAt = new AffineTransform();
        newAt.translate(dx, dy);
        newAt.concatenate(at);
        canvas.setRenderingTransform(newAt);
    }

    private void zoomAtPoint(double factor, Point p) {
        AffineTransform at = canvas.getRenderingTransform();
        if (at == null) {
            at = new AffineTransform();
        }
        AffineTransform newAt = new AffineTransform();
        newAt.translate(p.x, p.y);
        newAt.scale(factor, factor);
        newAt.translate(-p.x, -p.y);
        newAt.concatenate(at);
        canvas.setRenderingTransform(newAt);
        currentScale *= factor;
        updateStatus();
    }

    private void zoomAtCenter(double factor) {
        Point center = new Point(canvas.getWidth() / 2, canvas.getHeight() / 2);
        zoomAtPoint(factor, center);
    }

    private void fitToScreen() {
        GraphicsNode gn = canvas.getGraphicsNode();
        if (gn == null) {
            return;
        }
        Rectangle2D bounds = gn.getBounds();
        if (bounds == null || bounds.getWidth() <= 0 || bounds.getHeight() <= 0) {
            return;
        }

        Dimension dim = canvas.getSize();
        if (dim.width <= 0 || dim.height <= 0) {
            return;
        }

        double scaleX = dim.width / bounds.getWidth();
        double scaleY = dim.height / bounds.getHeight();
        double scale = Math.min(scaleX, scaleY) * 0.95; // small margin

        AffineTransform at = new AffineTransform();
        at.translate(dim.width / 2.0, dim.height / 2.0);
        at.scale(scale, scale);
        at.translate(-(bounds.getX() + bounds.getWidth() / 2.0),
                -(bounds.getY() + bounds.getHeight() / 2.0));

        currentScale = scale;
        canvas.setRenderingTransform(at);
    }

    private void actualSize() {
        GraphicsNode gn = canvas.getGraphicsNode();
        if (gn == null) {
            return;
        }
        Rectangle2D bounds = gn.getBounds();
        if (bounds == null) {
            return;
        }

        Dimension dim = canvas.getSize();
        AffineTransform at = new AffineTransform();
        at.translate(dim.width / 2.0, dim.height / 2.0);
        at.translate(-(bounds.getX() + bounds.getWidth() / 2.0),
                -(bounds.getY() + bounds.getHeight() / 2.0));

        currentScale = 1.0;
        canvas.setRenderingTransform(at);
    }

    private void updateStatus() {
        statusLabel.setText(String.format("%s    |    Zoom: %.0f%%", currentFileName, currentScale * 100));
    }

    // ---------------------------------------------------------------
    // Entry point
    // ---------------------------------------------------------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // fall back to default look and feel
            }
            new SvgViewer().setVisible(true);
        });
    }
}
