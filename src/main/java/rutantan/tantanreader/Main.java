/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rutantan.tantanreader;

import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.ScrollBarUI;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDNamedDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.printing.PDFPageable;
import org.apache.pdfbox.rendering.PDFRenderer;

/**
 *
 * @author cesar
 */
public class Main extends javax.swing.JFrame {

    PDFRenderer pdfRenderer;
    PDDocument document;
    PDPage page;
    BufferedImage image;
    String path;
    JLabel[] pageRenderedImage;
    float renderSize = 1;
    JButton buttons[];
    int currentPage;
    int leftPaneWidth;
    int miniatureWidth = -1, miniatureHeight = -1;
    int miniaturesVerticalScrollBarValue = 0;
    boolean miniaturesVerticalBarMouseReleased = false, pageVerticalBarMouseReleased = false;
    boolean hasLoadedDocument = false;
    boolean BookMode = false;
    ImageIcon whiteIcon;
    public static final Logger logger = Logger.getLogger(Main.class.getName());
    private static FileHandler fh = null;
    int[] bookmarksPages;
    boolean isLoadingDocument = false;

    public Main(String[] args) throws IOException {
        initComponents();
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("rutantan/tantanreader/Bundle");
        jTabbedPane1.setTitleAt(0, bundle.getString("Main.tab1.text"));
        jTabbedPane1.setTitleAt(1, bundle.getString("Main.tab2.text"));

        fh = new FileHandler(System.getProperty("user.home") + File.separator + "log.txt", false);
        fh.setFormatter(new SimpleFormatter());
        logger.addHandler(fh);
        logger.log(Level.INFO, "Starting program...");

        FlatIntelliJLaf.install();
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
            SwingUtilities.updateComponentTreeUI(Main.this);
            Main.this.pack();
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Failed to initialize LaF");
        }

        Main.this.setTitle("Tantan Reader");
        Main.this.setIconImage(new ImageIcon(getClass().getResource("/icon.png")).getImage());

        leftPaneWidth = jTabbedPane1.getWidth();
        TogglePanel();

        scrollPanePage.requestFocus();

        scrollPanePage.getViewport().addChangeListener(new Main.ListenAdditionsScrolled());
        scrollPaneMiniatures.getVerticalScrollBar().addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {
                miniaturesVerticalBarMouseReleased = true;

            }

            @Override
            public void mouseReleased(MouseEvent e) {
                miniaturesVerticalBarMouseReleased = true;
                try {
                    if ((scrollPaneMiniatures.getVerticalScrollBar().getValue() != miniaturesVerticalScrollBarValue) && miniaturesVerticalBarMouseReleased) {
                        miniaturesVerticalScrollBarValue = scrollPaneMiniatures.getVerticalScrollBar().getValue();
                        miniaturesVerticalBarMouseReleased = false;
                        int maxValue = scrollPaneMiniatures.getVerticalScrollBar().getMaximum();
                        int pageSelected = (miniaturesVerticalScrollBarValue * document.getNumberOfPages()) / maxValue;
                        setMiniatures(pageSelected + 2);
                    }
                } catch (Exception ex) {
                    System.out.println(ex);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });
        scrollPanePage.getVerticalScrollBar().addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {
                pageVerticalBarMouseReleased = true;
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });
        scrollPanePage.getVerticalScrollBar().addAdjustmentListener((e) -> {
            int extent = scrollPanePage.getVerticalScrollBar().getModel().getExtent();
            if (((e.getValue() + extent) == scrollPanePage.getVerticalScrollBar().getMaximum()) && pageVerticalBarMouseReleased) {
                nextPage();
                miniaturesVerticalBarMouseReleased = false;
            }
        });
        jTree1.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                TreePath tp = jTree1.getPathForLocation(me.getX(), me.getY());
                if (tp != null) {
                    Object[] path1 = tp.getPath();
                    try {
                        int indexOfChild = jTree1.getModel().getIndexOfChild(path1[0], path1[1]);
                        if (BookMode == false) {
                            loadPage(bookmarksPages[indexOfChild]);
                        } else {
                            if (bookmarksPages[indexOfChild] % 2 != 0) {
                                loadPage(bookmarksPages[indexOfChild], bookmarksPages[indexOfChild] + 1);
                            } else {
                                loadPage(bookmarksPages[indexOfChild] - 1, bookmarksPages[indexOfChild]);
                            }
                        }
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Load bookmark error: ", e);
                    }
                } else {

                }
            }
        });

        logger.log(Level.INFO, "Program started.");

        if (args.length > 0) {
            File f = new File(args[0]);
            if (f.isFile()) {
                if (f.getAbsolutePath().endsWith(".pdf")) {
                    loadPDF(f.getPath());
                }
            }
        }
    }

    public class ListenAdditionsScrolled implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent e) {
        }
    }

    private void loadPDF(String Path) {
        logger.log(Level.INFO, "Loading file " + Path);
        isLoadingDocument = true;
        File file = new File(Path);
        BookMode = tbBookMode.isSelected();
        panel.removeAll();
        try {
            scrollPanePage.getViewport().setViewPosition(new Point(0, 0));
            document = PDDocument.load(file);
            pdfRenderer = new PDFRenderer(document);
            lblLastPage.setText(String.valueOf(document.getNumberOfPages()));

            if (BookMode == false) {
                setupPageViewGrid(0);
            } else {
                if (document.getNumberOfPages() > 1) {
                    setupPageViewGrid(0, 1);
                } else {
                    BookMode = false;
                    tbBookMode.setSelected(false);
                    loadPDF(Path);
                }
            }
            setMiniaturesGrid();
            setMiniatures(currentPage);
            setupBookmarks();
            hasLoadedDocument = true;
            isLoadingDocument = false;
            logger.log(Level.INFO, "File loaded ");
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Load PDF error: ", ex);
        }

    }

    private void setupBookmarks() {
        PDDocumentOutline outline = document.getDocumentCatalog().getDocumentOutline();
        String indentation = "";
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) jTree1.getModel().getRoot();
        root.removeAllChildren();
        DefaultMutableTreeNode node = null;
        bookmarksPages = new int[1];
        int count = 0;
        try {
            if (outline != null) {
                PDOutlineItem current = outline.getFirstChild();
                while (current != null) {
                    PDDestination destination = current.getDestination();
                    if (current.getAction() instanceof PDActionGoTo) {
                        PDActionGoTo gta = (PDActionGoTo) current.getAction();
                        if (gta.getDestination() instanceof PDPageDestination) {
                            count++;
                        } else if (gta.getDestination() instanceof PDNamedDestination) {
                            PDPageDestination pd = document.getDocumentCatalog().findNamedDestinationPage((PDNamedDestination) gta.getDestination());
                            if (pd != null) {
                                count++;
                            }
                        }
                    }

                    current = current.getNextSibling();
                }
                current = outline.getFirstChild();
                bookmarksPages = new int[count];
                int i = 0;
                while (current != null) {
                    PDDestination destination = current.getDestination();
                    if (current.getAction() instanceof PDActionGoTo) {
                        PDActionGoTo gta = (PDActionGoTo) current.getAction();
                        if (gta.getDestination() instanceof PDPageDestination) {
                            PDPageDestination pd = (PDPageDestination) gta.getDestination();
                            bookmarksPages[i] = pd.retrievePageNumber();
                            i++;
                            node = new DefaultMutableTreeNode(current.getTitle());
                            root.add(node);
                        } else if (gta.getDestination() instanceof PDNamedDestination) {
                            PDPageDestination pd = document.getDocumentCatalog().findNamedDestinationPage((PDNamedDestination) gta.getDestination());
                            if (pd != null) {
                                bookmarksPages[i] = pd.retrievePageNumber();
                                i++;
                                node = new DefaultMutableTreeNode(current.getTitle());
                                root.add(node);
                            }
                        }
                    }

                    current = current.getNextSibling();
                }
            }
            DefaultTreeModel model = (DefaultTreeModel) jTree1.getModel();
            jTree1.setCellRenderer(
                    new DefaultTreeCellRenderer() {
                public Color getTextNonSelectionColor() {
                    return new Color(240, 240, 240);
                }
            });
            model.reload();

        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void setupPageViewGrid(int renderImage) {
        try {
            panel.removeAll();
            GridBagLayout grid = new GridBagLayout();
            panel.setLayout(grid);
            image = pdfRenderer.renderImage(renderImage, renderSize);
            pageRenderedImage = new JLabel[1];
            pageRenderedImage[0] = new JLabel();
            pageRenderedImage[0].setText("");
            pageRenderedImage[0].setVisible(true);
            pageRenderedImage[0].setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
            ImageIcon icon = new ImageIcon(image);
            pageRenderedImage[0].setIcon(icon);
            panel.add(pageRenderedImage[0]);
            setCurrentPage(renderImage);
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void setupPageViewGrid(int renderImage1, int renderImage2) {
        try {
            panel.removeAll();
            GridLayout grid = new GridLayout(1, 2, 0, 0);
            panel.setLayout(grid);
            pageRenderedImage = new JLabel[2];

            pageRenderedImage[0] = new JLabel();
            pageRenderedImage[0].setText("");
            pageRenderedImage[0].setVisible(true);

            pageRenderedImage[1] = new JLabel();
            pageRenderedImage[1].setText("");
            pageRenderedImage[1].setVisible(true);

            loadPage(renderImage1, renderImage2);

            panel.add(pageRenderedImage[0]);
            panel.add(pageRenderedImage[1]);

            pageRenderedImage[0].setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
            pageRenderedImage[1].setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
            setCurrentPage(renderImage1);
        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void loadPage(int page) {
        try {
            if (page >= 0 && page < document.getNumberOfPages()) {
                image = pdfRenderer.renderImage(page, renderSize);
                if (tbNightMode.isSelected()) {
                    image = invertColors(image);
                }
                lblLastPage.setText(String.valueOf(document.getNumberOfPages()));
                ImageIcon icon = new ImageIcon(image);
                pageRenderedImage[0].setIcon(icon);
                setCurrentPage(page);
                setMiniatures(currentPage);
                updateMiniaturesVerticalScrollBar();
                scrollPanePage.getVerticalScrollBar().setValue(0);
            }
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void loadPage(int page, int page1) {
        System.out.println("page: " + page + " page1: " + page1);
        try {
            ImageIcon icon;
            final int lastPage = document.getNumberOfPages() - 1;
            if (page == lastPage) {
                image = pdfRenderer.renderImage(page, renderSize);
                if (tbNightMode.isSelected()) {
                    image = invertColors(image);
                }
                lblLastPage.setText(String.valueOf(document.getNumberOfPages()));
                icon = new ImageIcon(image);
                pageRenderedImage[0].setIcon(icon);
                pageRenderedImage[1].setIcon(null);
            } else {
                switch (page) {
                    case -1:
                        pageRenderedImage[0].setIcon(null);
                        break;
                    case 0:
                        pageRenderedImage[0].setIcon(null);
                        break;
                    default:
                        if (page > 0 && page < document.getNumberOfPages()) {
                            image = pdfRenderer.renderImage(page, renderSize);
                            if (tbNightMode.isSelected()) {
                                image = invertColors(image);
                            }
                            lblLastPage.setText(String.valueOf(document.getNumberOfPages()));
                            icon = new ImageIcon(image);
                            pageRenderedImage[0].setIcon(icon);
                        }
                        break;
                }
            }
            switch (page1) {
                case -1:
                    pageRenderedImage[1].setIcon(null);
                    break;
                case -5:
                    image = pdfRenderer.renderImage(page, renderSize);
                    if (tbNightMode.isSelected()) {
                        image = invertColors(image);
                    }
                    lblLastPage.setText(String.valueOf(document.getNumberOfPages()));
                    icon = new ImageIcon(image);
                    pageRenderedImage[1].setIcon(icon);
                default:
                    if (page1 >= 0 && page1 < document.getNumberOfPages()) {
                        image = pdfRenderer.renderImage(page1, renderSize);
                        if (tbNightMode.isSelected()) {
                            image = invertColors(image);
                        }
                        lblLastPage.setText(String.valueOf(document.getNumberOfPages()));
                        icon = new ImageIcon(image);
                        pageRenderedImage[1].setIcon(icon);
                    }
                    break;
            }
            if (page > 0) {
                setCurrentPage(page);
            } else {
                setCurrentPage(0);
            }
            setMiniatures(currentPage);
            scrollPanePage.getVerticalScrollBar().setValue(0);
            updateMiniaturesVerticalScrollBar();
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private BufferedImage invertColors(BufferedImage image) {

        for (int i = 0; i < image.getHeight(); i++) {
            for (int j = 0; j < image.getWidth(); j++) {
                Color color = new Color(image.getRGB(j, i));
                if ((color.getRed() == 255) && (color.getRed() == 255) && (color.getRed() == 255)) {
                    image.setRGB(j, i, Color.BLACK.getRGB());
                }
                if ((color.getRed() == 0) && (color.getRed() == 0) && (color.getRed() == 0)) {
                    image.setRGB(j, i, Color.WHITE.getRGB());
                } else {
                    int red = 255 - color.getRed();
                    int blue = 255 - color.getBlue();
                    int green = 255 - color.getGreen();
                    color = new Color(red, green, blue);
                    image.setRGB(j, i, color.getRGB());
                }
            }
        }
        return image;
    }

    private void updateMiniaturesVerticalScrollBar() {
        int maxSize = scrollPaneMiniatures.getVerticalScrollBar().getMaximum();
        scrollPaneMiniatures.getVerticalScrollBar().setValue((maxSize / document.getNumberOfPages()) * (currentPage - 1));
    }

    private void setMiniaturesGrid() {
        panelMiniatures.removeAll();
        panelMiniatures.setLayout(new GridLayout(document.getNumberOfPages(), 1, 0, 10));
        if (hasLoadedDocument) {
            for (int i = 0; i < buttons.length; i++) {
                buttons[i].removeAll();
            }
            hasLoadedDocument = false;
        }
    }

    private void setMiniatures(int page) {
        if (!hasLoadedDocument) {
            buttons = new JButton[document.getNumberOfPages()];
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                buttons[i] = new JButton();
                buttons[i].setVisible(true);
                buttons[i].setBorderPainted(true);
                buttons[i].setContentAreaFilled(false);
                buttons[i].setToolTipText(String.valueOf(i + 1));
                buttons[i].setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                panelMiniatures.add(buttons[i]);
                final int alfa = i;
                buttons[i].addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e
                    ) {
                        if (e.getButton() == MouseEvent.BUTTON1) {

                            if (BookMode == false) {
                                loadPage(alfa);
                            } else {
                                if (alfa % 2 != 0) {
                                    // is odd
                                    loadPage(alfa, alfa + 1);
                                } else {
                                    // is even
                                    loadPage(alfa - 1, alfa);
                                }
                            }
                            setMiniatures(currentPage);
                        }
                    }
                });
            }
        }
        int pageLoadMin = page - 3;
        int pageLoadMax = page + 3;
        for (int i = 0; i < document.getNumberOfPages(); i++) {
            try {
                if ((i >= pageLoadMin) && (i <= pageLoadMax)) {
                    if (isLoadingDocument) {
                        image = pdfRenderer.renderImage(i, (float) 0.20);
                        if (tbNightMode.isSelected()) {
                            image = invertColors(image);
                        }
                        ImageIcon icon = new ImageIcon(image);
                        buttons[i].setIcon(icon);
                        miniatureHeight = icon.getIconHeight();
                        miniatureWidth = icon.getIconWidth();
                    } else {
                        if (buttons[i].getIcon() == whiteIcon) {
                            image = pdfRenderer.renderImage(i, (float) 0.20);
                            if (tbNightMode.isSelected()) {
                                image = invertColors(image);
                            }
                            ImageIcon icon = new ImageIcon(image);
                            buttons[i].setIcon(icon);
                            miniatureHeight = icon.getIconHeight();
                            miniatureWidth = icon.getIconWidth();
                        }
                    }
                } else {
                    if (miniatureHeight != -1) {
                        if (!hasLoadedDocument) {
                            image = new BufferedImage(miniatureWidth, miniatureHeight, BufferedImage.TYPE_INT_RGB);
                            Graphics2D graphics = image.createGraphics();
                            graphics.setPaint(new Color(255, 255, 255));
                            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
                            whiteIcon = new ImageIcon(image);
                        }
                        buttons[i].setIcon(whiteIcon);
                    } else {
                        buttons[i].setIcon(null);
                    }
                }

            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        ;
        scrollPaneMiniatures.getVerticalScrollBar().setValue(scrollPaneMiniatures.getVerticalScrollBar().getValue() + 30);
        scrollPaneMiniatures.getVerticalScrollBar().setValue(scrollPaneMiniatures.getVerticalScrollBar().getValue() - 30);
    }

    private void setCurrentPage(int page) {
        currentPage = page + 1;
        txtPage.setText(String.valueOf(page + 1));
    }

    private void BookMode() {
        renderSize = (float) 0.75;
        try {
            if (BookMode != tbBookMode.isSelected() && document.getNumberOfPages() > 1) {
                if (tbBookMode.isSelected() == true) {
                    if (currentPage - 1 == 0) {
                        setupPageViewGrid(-1, currentPage - 1);
                    } else {
                        if (currentPage % 2 == 0) {
                            // if page number is even
                            setupPageViewGrid(currentPage - 1, currentPage);
                        } else {
                            // if page number is odd
                            setupPageViewGrid(currentPage - 2, currentPage - 1);
                        }
                    }
                } else {
                    setupPageViewGrid(currentPage - 1);
                }
            }
            BookMode = tbBookMode.isSelected();
            scrollPanePage.getVerticalScrollBar().setValue(scrollPanePage.getVerticalScrollBar().getValue() + 30);
            scrollPanePage.getVerticalScrollBar().setValue(scrollPanePage.getVerticalScrollBar().getValue() - 30);
        } catch (Exception e) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Book mode error: ", e);
        }
    }

    private void TogglePanel() {
        if (tbTogglePanel.isSelected()) {
            jTabbedPane1.setVisible(true);
            scrollPanePage.setSize((scrollPanePage.getWidth() - leftPaneWidth), scrollPanePage.getHeight());
            jPanel1.updateUI();

        } else {
            jTabbedPane1.setVisible(false);
            scrollPanePage.setSize((scrollPanePage.getWidth() + leftPaneWidth), scrollPanePage.getHeight());
            jPanel1.updateUI();
        }
    }

    private void zoomIn() {
        if (renderSize < 2) {
            renderSize = renderSize + (float) 0.25;
            if (BookMode == false) {
                loadPage(currentPage - 1);
            } else {
                loadPage(currentPage - 1, currentPage);
            }
        }
    }

    private void zoomOut() {
        if (renderSize >= 0.5) {
            renderSize = renderSize - (float) 0.25;
            if (BookMode == false) {
                loadPage(currentPage - 1);
            } else {
                loadPage(currentPage - 1, currentPage);
            }
        }
    }

    private void zoom(double zoom) {
        renderSize = (float) zoom;
        if (BookMode == false) {
            loadPage(currentPage - 1);
        } else {
            loadPage(currentPage - 1, currentPage);
        }
    }

    private void nextPage() {
        if (hasLoadedDocument) {
            if (BookMode == false) {
                if (currentPage + 1 <= document.getNumberOfPages()) {
                    loadPage(currentPage);
                }
            } else {
                if (currentPage + 2 == document.getNumberOfPages()) {
                    loadPage(currentPage + 2, -1);
                } else if (currentPage % 2 != 0) {
                    // is odd
                    if (currentPage + 1 < document.getNumberOfPages()) {
                        loadPage(currentPage, currentPage + 1);
                    }
                } else {
                    // is even
                    if (currentPage + 2 < document.getNumberOfPages()) {
                        loadPage(currentPage + 1, currentPage + 2);
                    }
                }
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jPanel1 = new javax.swing.JPanel();
        toolbarPanel = new javax.swing.JPanel();
        jToolBar2 = new javax.swing.JToolBar();
        btnOpen = new javax.swing.JButton();
        btnPrint = new javax.swing.JButton();
        jToolBar1 = new javax.swing.JToolBar();
        btnFirstPage = new javax.swing.JButton();
        btnPreviousPage = new javax.swing.JButton();
        txtPage = new javax.swing.JFormattedTextField();
        lblPageSeparator = new javax.swing.JLabel();
        lblLastPage = new javax.swing.JLabel();
        btnNextPage = new javax.swing.JButton();
        btnLastPage = new javax.swing.JButton();
        jToolBar3 = new javax.swing.JToolBar();
        tbBookMode = new javax.swing.JToggleButton();
        tbTogglePanel = new javax.swing.JToggleButton();
        tbNightMode = new javax.swing.JToggleButton();
        btnZoomOut = new javax.swing.JButton();
        btnZoomIn = new javax.swing.JButton();
        scrollPanePage = new javax.swing.JScrollPane();
        panel = new javax.swing.JPanel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        scrollPaneMiniatures = new javax.swing.JScrollPane();
        panelMiniatures = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTree1 = new javax.swing.JTree();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setBackground(new java.awt.Color(51, 51, 51));
        setMinimumSize(new java.awt.Dimension(660, 500));
        setPreferredSize(new java.awt.Dimension(800, 500));

        toolbarPanel.setBackground(new java.awt.Color(51, 51, 51));
        toolbarPanel.setLayout(new java.awt.GridBagLayout());

        jToolBar2.setBorder(null);
        jToolBar2.setFloatable(false);
        jToolBar2.setRollover(true);
        jToolBar2.setBorderPainted(false);

        btnOpen.setBackground(new java.awt.Color(51, 51, 51));
        btnOpen.setIcon(new javax.swing.ImageIcon(getClass().getResource("/open20.png"))); // NOI18N
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("rutantan/tantanreader/Bundle"); // NOI18N
        btnOpen.setToolTipText(bundle.getString("Main.btnOpen.tooltip")); // NOI18N
        btnOpen.setFocusable(false);
        btnOpen.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnOpen.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOpenActionPerformed(evt);
            }
        });
        jToolBar2.add(btnOpen);

        btnPrint.setBackground(new java.awt.Color(51, 51, 51));
        btnPrint.setIcon(new javax.swing.ImageIcon(getClass().getResource("/print20.png"))); // NOI18N
        btnPrint.setToolTipText(bundle.getString("Main.btnPrint.tooltip")); // NOI18N
        btnPrint.setFocusable(false);
        btnPrint.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnPrint.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnPrint.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPrintActionPerformed(evt);
            }
        });
        jToolBar2.add(btnPrint);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.1;
        toolbarPanel.add(jToolBar2, gridBagConstraints);

        jToolBar1.setBackground(new java.awt.Color(51, 51, 51));
        jToolBar1.setBorder(null);
        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);
        jToolBar1.setBorderPainted(false);
        jToolBar1.setMaximumSize(new java.awt.Dimension(517, 25));
        jToolBar1.setMinimumSize(new java.awt.Dimension(505, 25));

        btnFirstPage.setBackground(new java.awt.Color(51, 51, 51));
        btnFirstPage.setIcon(new javax.swing.ImageIcon(getClass().getResource("/firstPage20.png"))); // NOI18N
        btnFirstPage.setToolTipText(bundle.getString("Main.btnFirstPage.tooltip")); // NOI18N
        btnFirstPage.setFocusable(false);
        btnFirstPage.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnFirstPage.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnFirstPage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFirstPageActionPerformed(evt);
            }
        });
        jToolBar1.add(btnFirstPage);

        btnPreviousPage.setBackground(new java.awt.Color(51, 51, 51));
        btnPreviousPage.setIcon(new javax.swing.ImageIcon(getClass().getResource("/previousPage20.png"))); // NOI18N
        btnPreviousPage.setToolTipText(bundle.getString("Main.btnPreviousPage.tooltip")); // NOI18N
        btnPreviousPage.setFocusable(false);
        btnPreviousPage.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnPreviousPage.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnPreviousPage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPreviousPageActionPerformed(evt);
            }
        });
        jToolBar1.add(btnPreviousPage);

        txtPage.setBackground(new java.awt.Color(102, 102, 102));
        txtPage.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        txtPage.setForeground(new java.awt.Color(204, 204, 204));
        txtPage.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtPage.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        txtPage.setMaximumSize(new java.awt.Dimension(50, 100));
        txtPage.setMinimumSize(new java.awt.Dimension(50, 800));
        txtPage.setPreferredSize(new java.awt.Dimension(50, 23));
        txtPage.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txtPageKeyPressed(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtPageKeyReleased(evt);
            }
        });
        jToolBar1.add(txtPage);

        lblPageSeparator.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        lblPageSeparator.setForeground(new java.awt.Color(204, 204, 204));
        lblPageSeparator.setText(" / "); // NOI18N
        jToolBar1.add(lblPageSeparator);

        lblLastPage.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        lblLastPage.setForeground(new java.awt.Color(204, 204, 204));
        lblLastPage.setText("1000"); // NOI18N
        jToolBar1.add(lblLastPage);

        btnNextPage.setBackground(new java.awt.Color(51, 51, 51));
        btnNextPage.setIcon(new javax.swing.ImageIcon(getClass().getResource("/nextPage20.png"))); // NOI18N
        btnNextPage.setToolTipText(bundle.getString("Main.btnNextPage.tooltip")); // NOI18N
        btnNextPage.setFocusable(false);
        btnNextPage.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnNextPage.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnNextPage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNextPageActionPerformed(evt);
            }
        });
        jToolBar1.add(btnNextPage);

        btnLastPage.setBackground(new java.awt.Color(51, 51, 51));
        btnLastPage.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lastPage20.png"))); // NOI18N
        btnLastPage.setToolTipText(bundle.getString("Main.btnLastPage.tooltip")); // NOI18N
        btnLastPage.setFocusable(false);
        btnLastPage.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnLastPage.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnLastPage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLastPageActionPerformed(evt);
            }
        });
        jToolBar1.add(btnLastPage);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        toolbarPanel.add(jToolBar1, gridBagConstraints);

        jToolBar3.setBorder(null);
        jToolBar3.setFloatable(false);
        jToolBar3.setRollover(true);
        jToolBar3.setBorderPainted(false);

        tbBookMode.setBackground(new java.awt.Color(51, 51, 51));
        tbBookMode.setIcon(new javax.swing.ImageIcon(getClass().getResource("/multipage20.png"))); // NOI18N
        tbBookMode.setToolTipText(bundle.getString("Main.btnBookMode.tooltip")); // NOI18N
        tbBookMode.setFocusable(false);
        tbBookMode.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        tbBookMode.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        tbBookMode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tbBookModeActionPerformed(evt);
            }
        });
        jToolBar3.add(tbBookMode);

        tbTogglePanel.setBackground(new java.awt.Color(51, 51, 51));
        tbTogglePanel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/panel20.png"))); // NOI18N
        tbTogglePanel.setSelected(true);
        tbTogglePanel.setToolTipText(bundle.getString("Main.btPanel.tooltip")); // NOI18N
        tbTogglePanel.setFocusable(false);
        tbTogglePanel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        tbTogglePanel.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        tbTogglePanel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tbTogglePanelActionPerformed(evt);
            }
        });
        jToolBar3.add(tbTogglePanel);

        tbNightMode.setBackground(new java.awt.Color(51, 51, 51));
        tbNightMode.setIcon(new javax.swing.ImageIcon(getClass().getResource("/nightmode20.png"))); // NOI18N
        tbNightMode.setToolTipText(bundle.getString("Main.tbNightMode.toolTipText")); // NOI18N
        tbNightMode.setFocusable(false);
        tbNightMode.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        tbNightMode.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        tbNightMode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tbNightModeActionPerformed(evt);
            }
        });
        jToolBar3.add(tbNightMode);

        btnZoomOut.setBackground(new java.awt.Color(51, 51, 51));
        btnZoomOut.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        btnZoomOut.setIcon(new javax.swing.ImageIcon(getClass().getResource("/zoomOut20.png"))); // NOI18N
        btnZoomOut.setToolTipText(bundle.getString("Main.btnZoomOut.tooltip")); // NOI18N
        btnZoomOut.setFocusable(false);
        btnZoomOut.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnZoomOut.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnZoomOut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnZoomOutActionPerformed(evt);
            }
        });
        jToolBar3.add(btnZoomOut);

        btnZoomIn.setBackground(new java.awt.Color(51, 51, 51));
        btnZoomIn.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        btnZoomIn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/zoomIn20.png"))); // NOI18N
        btnZoomIn.setToolTipText(bundle.getString("Main.btnZoomIn.tooltip")); // NOI18N
        btnZoomIn.setFocusable(false);
        btnZoomIn.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnZoomIn.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnZoomIn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnZoomInActionPerformed(evt);
            }
        });
        jToolBar3.add(btnZoomIn);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        toolbarPanel.add(jToolBar3, gridBagConstraints);

        scrollPanePage.setBorder(null);

        panel.setBackground(new java.awt.Color(51, 51, 51));
        panel.setLayout(new java.awt.GridBagLayout());
        scrollPanePage.setViewportView(panel);

        jTabbedPane1.setBackground(new java.awt.Color(51, 51, 51));
        jTabbedPane1.setForeground(new java.awt.Color(240, 240, 240));

        scrollPaneMiniatures.setBorder(null);
        scrollPaneMiniatures.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        panelMiniatures.setBackground(new java.awt.Color(51, 51, 51));
        panelMiniatures.setMaximumSize(new java.awt.Dimension(32767, 200));

        javax.swing.GroupLayout panelMiniaturesLayout = new javax.swing.GroupLayout(panelMiniatures);
        panelMiniatures.setLayout(panelMiniaturesLayout);
        panelMiniaturesLayout.setHorizontalGroup(
            panelMiniaturesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 169, Short.MAX_VALUE)
        );
        panelMiniaturesLayout.setVerticalGroup(
            panelMiniaturesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 528, Short.MAX_VALUE)
        );

        scrollPaneMiniatures.setViewportView(panelMiniatures);

        jTabbedPane1.addTab("tab1", scrollPaneMiniatures);

        jTree1.setBackground(new java.awt.Color(51, 51, 51));
        jTree1.setForeground(new java.awt.Color(255, 255, 255));
        jTree1.setName(""); // NOI18N
        jTree1.setRootVisible(false);
        jScrollPane1.setViewportView(jTree1);

        jTabbedPane1.addTab(bundle.getString("Main.jScrollPane1.TabConstraints.tabTitle"), jScrollPane1); // NOI18N

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(scrollPanePage))
            .addComponent(toolbarPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 845, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(toolbarPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(scrollPanePage, javax.swing.GroupLayout.DEFAULT_SIZE, 528, Short.MAX_VALUE)
                    .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)))
        );

        jTabbedPane1.getAccessibleContext().setAccessibleName(bundle.getString("Main.tab2.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void btnZoomInActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnZoomInActionPerformed
        if (hasLoadedDocument) {
            zoomIn();
        }
    }//GEN-LAST:event_btnZoomInActionPerformed

    private void btnZoomOutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnZoomOutActionPerformed
        if (hasLoadedDocument) {
            zoomOut();
        }
    }//GEN-LAST:event_btnZoomOutActionPerformed

    private void tbTogglePanelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tbTogglePanelActionPerformed

        TogglePanel();
    }//GEN-LAST:event_tbTogglePanelActionPerformed

    private void tbBookModeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tbBookModeActionPerformed
        if (hasLoadedDocument) {
            if (document.getNumberOfPages() > 1) {
                BookMode();
            } else {
                tbBookMode.setSelected(false);
            }
        }
    }//GEN-LAST:event_tbBookModeActionPerformed

    private void btnLastPageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLastPageActionPerformed
        if (hasLoadedDocument) {
            if (BookMode == false) {
                loadPage(document.getNumberOfPages() - 1);
            } else {
                if ((document.getNumberOfPages() - 1) % 2 == 0) {
                    //if last page is even
                    loadPage(document.getNumberOfPages() - 1, -1);
                } else {
                    // if last page is odd
                    loadPage(document.getNumberOfPages() - 2, document.getNumberOfPages() - 1);
                }
            }
            updateMiniaturesVerticalScrollBar();
        }
    }//GEN-LAST:event_btnLastPageActionPerformed

    private void btnNextPageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNextPageActionPerformed
        nextPage();
    }//GEN-LAST:event_btnNextPageActionPerformed

    private void txtPageKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtPageKeyPressed

    }//GEN-LAST:event_txtPageKeyPressed

    private void btnPreviousPageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPreviousPageActionPerformed
        if (hasLoadedDocument) {
            if (BookMode == false) {
                if (currentPage - 2 >= 0) {
                    loadPage(currentPage - 2);
                }
            } else {
                if (currentPage == 2) {
                    //  show first page on right side
                    loadPage(-1, currentPage - 2);
                } else if (currentPage % 2 != 0) {
                    if (currentPage > 2) {
                        loadPage(currentPage - 3, currentPage - 2);
                    } else {
                        loadPage(currentPage - 4, currentPage - 3);
                    }
                } else {
                    if (currentPage > 2) {
                        loadPage(currentPage - 3, currentPage - 2);
                    } else {
                        loadPage(currentPage - 4, currentPage - 3);
                    }
                }
            }
        }
    }//GEN-LAST:event_btnPreviousPageActionPerformed

    private void btnFirstPageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFirstPageActionPerformed
        if (hasLoadedDocument) {
            if (BookMode == false) {
                loadPage(0);
            } else {
                loadPage(0, 1);
            }
        }
    }//GEN-LAST:event_btnFirstPageActionPerformed

    private void btnPrintActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPrintActionPerformed
        if (hasLoadedDocument) {
            PrinterJob job = PrinterJob.getPrinterJob();

            job.setPageable(new PDFPageable(document));

            if (job.printDialog()) {
                try {
                    job.print();
                } catch (PrinterException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Print error: ", ex);
                }
            }
        }
    }//GEN-LAST:event_btnPrintActionPerformed

    private void btnOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOpenActionPerformed
        final JFileChooser openFile = new JFileChooser();
        openFile.setMultiSelectionEnabled(false);
        //        saveAs.setApproveButtonText(LanguageClass.translation[29]);
        //        saveAs.setApproveButtonToolTipText(LanguageClass.translation[29]);
        int actionDialog = openFile.showOpenDialog(this);
        if (actionDialog == JFileChooser.APPROVE_OPTION) {
            String alfa = openFile.getSelectedFile().getAbsolutePath();
            Main.this.setTitle("Tantan reader - " + openFile.getSelectedFile().getName());
            if (alfa.endsWith(".pdf")) {
                if (hasLoadedDocument) {
                    try {
                        document.close();
                    } catch (IOException ex) {
                        logger.log(Level.WARNING, "Error closing document: " + ex.toString(), ex);
                    }
                }
                loadPDF(alfa);
            } else {
                //                JOptionPane.showMessageDialog(null, LanguageClass.translation[30]);
            }
        }
    }//GEN-LAST:event_btnOpenActionPerformed

    private void tbNightModeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tbNightModeActionPerformed
        if (hasLoadedDocument) {
            if (BookMode == false) {
                loadPage(currentPage - 1);
            } else {
                loadPage(currentPage - 1, currentPage);
            }
            updateMiniaturesVerticalScrollBar();
            setMiniatures(currentPage);
        }
    }//GEN-LAST:event_tbNightModeActionPerformed

    private void txtPageKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtPageKeyReleased
        if (hasLoadedDocument) {
            if ("10".equals(String.valueOf(evt.getKeyCode()))) { //event code for enter key
                try {
                    int i = Integer.parseInt(txtPage.getText().replace(" ", ""));
                    if (BookMode == false) {
                        loadPage(i - 1);
                    } else {
                        if (i % 2 != 0) {
                            // is odd
                            loadPage(i - 2, i - 1);
                        } else {
                            // is even
                            loadPage(i - 1, i);
                        }
                    }
                } catch (Exception ex) {
                    System.out.println(ex);
                }
            }
        }
    }//GEN-LAST:event_txtPageKeyReleased

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Main.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Main.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Main.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Main.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    new Main(args).setVisible(true);
                } catch (IOException ex) {
                    Logger.getLogger(Main.class
                            .getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnFirstPage;
    private javax.swing.JButton btnLastPage;
    private javax.swing.JButton btnNextPage;
    private javax.swing.JButton btnOpen;
    private javax.swing.JButton btnPreviousPage;
    private javax.swing.JButton btnPrint;
    private javax.swing.JButton btnZoomIn;
    private javax.swing.JButton btnZoomOut;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JToolBar jToolBar2;
    private javax.swing.JToolBar jToolBar3;
    private javax.swing.JTree jTree1;
    private javax.swing.JLabel lblLastPage;
    private javax.swing.JLabel lblPageSeparator;
    private javax.swing.JPanel panel;
    private javax.swing.JPanel panelMiniatures;
    private javax.swing.JScrollPane scrollPaneMiniatures;
    private javax.swing.JScrollPane scrollPanePage;
    private javax.swing.JToggleButton tbBookMode;
    private javax.swing.JToggleButton tbNightMode;
    private javax.swing.JToggleButton tbTogglePanel;
    private javax.swing.JPanel toolbarPanel;
    private javax.swing.JFormattedTextField txtPage;
    // End of variables declaration//GEN-END:variables
}
