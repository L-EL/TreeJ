//package runTreeJ;
/**
 * @see https://imagej.net/plugins/treej
 * @author Elise laruelle
 * @since 2015
 * @version v2 (includes the annotation feature)
 */
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.gui.StackWindow;
import ij.io.LogStream;
import ij.io.DirectoryChooser;
import ij.plugin.PlugIn;
import ij.process.ImageConverter;
import ij.WindowManager;
import ij.measure.Calibration;

import javax.swing.BorderFactory;
import javax.swing.JTextField;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JButton;
import javax.swing.plaf.basic.BasicFileChooserUI;
import javax.swing.plaf.FileChooserUI;
import javax.swing.SwingUtilities;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.event.WindowEvent;
import java.awt.Checkbox;
import java.awt.Dimension;
import java.awt.TextField;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

import java.lang.System;

import java.io.PrintWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.io.FileOutputStream;
import java.io.StringWriter;
import java.io.BufferedReader;
import java.io.FileReader;

import java.lang.Math;

import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.DocumentException;

public class TreeJ_ implements PlugIn {


	/**
	 * This class create node to build the tree.
	 * adapted from http://stackoverflow.com/questions/4965335/how-to-print-binary-tree-diagram/8948691#8948691
	 * 
	 */
	
	public static class Node {

		Integer value;
		String Note;
		Node left, right;

		public Node() {

		}

		public Node(int cellLabel) {
			this.value = cellLabel;
			this.Note = "";
		}

		public Node(int motherLabel, Node daughter1, Node daughter2) {
			this.value = motherLabel;
			this.left = daughter1;
			this.right = daughter2;
			this.Note = "";
		}

		public Node(int motherLabel, Node daughter1, Node daughter2, String note) {
			this.value = motherLabel;
			this.left = daughter1;
			this.right = daughter2;
			this.Note = note;
		}

		public void insertOneDaughter(Node Daughter) {

			if (this.left == null) {
				this.left = Daughter;
			} else if (this.right == null) {
				this.right = Daughter;
			} else {
				IJ.log("erreur : more than 2 daughter cells ?! ");
			}
		}

		public void printTree(StringWriter out) {
			if (this.right != null) {
				this.right.printTree(out, true, "");
			}
			printNodeValue(out);
			if (this.left != null) {
				this.left.printTree(out, false, "");
			}
		}

		private void printNodeValue(StringWriter out) {
			if (this.value == null) {
				out.write("<null>");
			} else {
				if (this.Note.isEmpty()) {
					out.write(Integer.toString(this.value));
				} else {
					out.write(this.Note + "(" + Integer.toString(this.value) + ")");
				}
			}
			out.write('\n');
		}

		// use string and not stringbuffer on purpose as we need to change the indent at
		// each recursion
		private void printTree(StringWriter out, boolean isRight, String indent) {
			if (this.right != null) {
				this.right.printTree(out, true, indent + (isRight ? "        " : " |      "));
			}

			out.write(indent);
			if (isRight) {
				out.write(" /");
			} else {
				out.write(" \\");
			}
			out.write("----- ");
			printNodeValue(out);
			if (this.left != null) {
				this.left.printTree(out, false, indent + (isRight ? " |      " : "        "));
			}
		}
	}

// ------------------ End Node class
	/**
	 * The Tree class contains attributes and methods to build the tree and to maintain it. 
	 * @param fathers contains the mother cell label per cell (the treeV format)
	 * @param notes contains the annotation per cell (includes in the treeV format)
	 * @param listNode contains the node address per cell 
	 * @param nextFather is the next label number 
	 */
	public static class Tree {

		int max;
		// variables to use for the Lineage
		java.util.List<Integer> fathers;
		java.util.List<String> notes;
		java.util.List<Node> listNode;
		int nextFather;
		StringWriter str;

		public Tree() {
			this.max = 0;
		}

		public void initialize(ImageStack inputStackCopy) {

			TreeSet<Integer> labelsTree = new TreeSet<Integer>();// from David Legland
																	// ijpb-plugins/src/main/java/inra/ijpb/morphology/LabelImages.java
			int[] array;

			int sizeX = inputStackCopy.getWidth();
			int sizeY = inputStackCopy.getHeight();
			int sizeZ = inputStackCopy.getSize();
			IJ.log("step 3");

			// iterate on image pixels
			for (int z = 0; z < sizeZ; z++) {
				IJ.showProgress(z, sizeZ);
				for (int y = 0; y < sizeY; y++)
					for (int x = 0; x < sizeX; x++)
						labelsTree.add((int) inputStackCopy.getVoxel(x, y, z));
			}
			IJ.showProgress(1);
			IJ.log("step 4");
			/**
			 * remove 0 if it exists if (labels.contains(0)) labels.remove(0);
			 **/
			// convert to array of integers

			array = new int[labelsTree.size()];
			this.fathers = new ArrayList<Integer>(labelsTree.size());
			this.notes = new ArrayList<String>(labelsTree.size() + 1);
			Iterator<Integer> iterator = labelsTree.iterator();
			IJ.log("step 5");
			for (int i = 0; i < labelsTree.size(); i++) {
				array[i] = iterator.next();

				if (array[i] > this.max) {
					this.max = array[i];
				}
			}
			this.listNode = new ArrayList<Node>();
			IJ.log("step 6");
			for (int i = 0; i <= this.max; i++) {
				this.newEmptyCell(i);
			}
			// notes.add("");
			IJ.log("nb of label " + labelsTree.size() + "");
			IJ.log("label number max :  " + max + "");
			IJ.log("nb of label (with zero (as background?)) :  " + fathers.size() + "");
			IJ.log("step 7");

			this.nextFather = this.max + 1;
			this.str = new StringWriter();

		}

		public void newEmptyCell(int label) {
			this.listNode.add(new Node(label));
			this.fathers.add(0);
			this.notes.add("");
		}

		public void newMotherCell(int label, Node Daughter1, Node Daughter2, String note) {
			this.listNode.add(new Node(label, Daughter1, Daughter2, note));
			this.fathers.add(0);
			this.notes.add(note);
		}

		public void resetTree() {
			for (int i = 0; i < this.listNode.size(); i++) {
				this.listNode.get(i).left = null;
				this.listNode.get(i).right = null;
				this.listNode.get(i).Note = "";
			}
		}

		public void clearTree() {
			for (int i = 0; i < this.listNode.size(); i++) {
				this.fathers.clear();
				this.notes.clear();
				this.resetTree();
			}
		}

		public void unlinkFromMother(int motherLabel) {
			do {
				this.listNode.get(motherLabel).left = null;
				this.listNode.get(motherLabel).right = null;
				this.listNode.get(motherLabel).Note = "";
				this.notes.set(motherLabel, "");

				for (int i = 0; i < this.fathers.size(); i++) {
					if (this.fathers.get(i) == motherLabel) {
						this.fathers.set(i, 0);
					}
				}
				motherLabel = this.fathers.get(motherLabel);
			} while (motherLabel != 0);

		}

		public void writeStrTree() {
			this.str.getBuffer().setLength(0);
			for (int i = 0; i < this.fathers.size(); i++) {
				if (this.fathers.get(i) == 0 && this.listNode.get(i).left != null) {
					this.listNode.get(i).printTree(this.str);
				}
			}
		}

		public void writeToPDF(String filePathName) {
			this.writeStrTree();

			Document document = new Document();
			try {
				PdfWriter.getInstance(document, new FileOutputStream(filePathName));
			} catch (DocumentException e) {
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

			document.open();
			try {
				document.add(new Paragraph(this.str.toString()));
			} catch (DocumentException e) {
				e.printStackTrace();
			}
			document.close();
		}

		public void writeToTreeV(File file) {

			try (PrintWriter writer = new PrintWriter(file)) {
				int i;
				for (i = 1; i < this.fathers.size(); i++) {
					writer.print(Integer.toString(this.fathers.get(i)));
					writer.print("\t");
				}

				writer.print("\n");
				for (i = 1; i < this.fathers.size(); i++) {
					writer.print(this.notes.get(i));
					writer.print("\t");
				}

				writer.close();
				IJ.log("file save as : " + file.getName());
			} catch (FileNotFoundException ex) {
				IJ.log("error");
				System.out.println("file error in filiationTool");
				IJ.error("file error in filiationTool", "FileNotFoundException");
			}
		}

		public void writeTreeToNwk(File file) {
			try {

				PrintWriter writer = new PrintWriter(file);
				for (int i = 1; i < this.fathers.size(); i++) {
					if (this.fathers.get(i) == 0 && this.listNode.get(i).left != null) {
						IJ.log("root" + i);
						String a = this.buildStrTree(this.listNode.get(i)) + ";\n";
						writer.print(a);
					}
				}
				writer.close();
				IJ.log("file save as : " + file.getName());
			} catch (FileNotFoundException ex) {
				IJ.log("error");
				System.out.println("file error in filiationTool");
				IJ.error("file error in filiationTool", "FileNotFoundException");
			}
		}

		private String buildStrTree(Node t) {
			// for nwk format
			String strTree = "";

			if (t.left == null && t.right == null) {
				IJ.log(Integer.toString(t.value));
				strTree = Integer.toString(t.value);
			} else {
				strTree = "(" + this.buildStrTree(t.left) + "," + this.buildStrTree(t.right) + ")"
						+ Integer.toString(t.value);
			}
			return strTree;
		}

		public void readTreeV(String file) {
			try {
				BufferedReader br = new BufferedReader(new FileReader(file));
				try {
					String line = br.readLine();
					int lineNumber = 0;
					while (line != null) {
						if (lineNumber == 0) {
							String everything = line.toString();
							// System.out.println(everything);
							String[] everythingParse = everything.split("[\t| ]");
							// System.out.println(everythingParse.length);

							// fathers = new ArrayList<Integer>(everythingParse.length);
							this.fathers.add(0); // <--- because the treeV array mean [P(1) P(2) ...P(n)] where P() mean
													// the father and here father is [P(index)] (index start from zero)
							this.notes.add("");
							for (int i = 0; i < everythingParse.length; i++) {
								this.fathers.add(Integer.parseInt(everythingParse[i]));
								this.notes.add("");
							}
						} else if (lineNumber == 1) {
							// extract notes
							String everything = line.toString();
							// System.out.println(everything);
							String tmp = "";
							int i = 1;
							while (everything.length() != 0) {
								if (i > this.fathers.size()) {
									System.out.println("error : more tags than cells ");
								}
								if (everything.charAt(0) != '\t') {
									tmp = tmp.concat(String.valueOf(everything.charAt(0)));

								} else {
									this.notes.set(i, tmp);
									tmp = "";
									i = i + 1;
								}
								everything = everything.substring(1);
							}
							/*
							 * // alternative way for (int i = 1; i < fathers.size(); i++) {
							 * 
							 * if ( everything.charAt(0) != '\t') {
							 * notes.set(i,String.valueOf(everything.charAt(0))); everything =
							 * everything.substring(1); listNode.get(i).Note =
							 * String.valueOf(everything.charAt(0)); } everything = everything.substring(1);
							 * 
							 * }
							 */
						} else {
							System.out.println("error : more than two lines in the treeV file : not processed ");
						}
						line = br.readLine();
						lineNumber = lineNumber + 1;
					}
					br.close();
					// notes.add("");
				} catch (IOException ex) {
					System.out.println("error :IOException");
					return;
				}
			} catch (FileNotFoundException ex) {
				System.out.println("error :FileNotFoundException");

				return;
			}
		}

		public void readNwk(String file) {
			try {
				BufferedReader br = new BufferedReader(new FileReader(file));
				try {
					String line = br.readLine();
					br.close();
					// parse string :
					java.util.List<String> everythingParse = new ArrayList<String>();
					String tmp = "";
					int fmax = 0;
					while (line.length() != 0) {
						if (Character.isDigit(line.charAt(0))) {
							while (Character.isDigit(line.charAt(0))) {
								tmp = tmp.concat(String.valueOf(line.charAt(0)));
								line = line.substring(1);
							}
							everythingParse.add(tmp);

							if (Integer.parseInt(tmp) > fmax) {
								fmax = Integer.parseInt(tmp);
							}
							tmp = "";
						} else {
							everythingParse.add(String.valueOf(line.charAt(0)));
							line = line.substring(1);
						}

					}
					System.out.println(everythingParse);
					// create null father list :
					for (int i = 0; i <= fmax; i++) {
						this.fathers.add(0);
						this.notes.add("");
					}
					// search the father for each label :
					int i = 0;
					for (int c = 0; c < everythingParse.size(); c++) {
						if (Character.isDigit(everythingParse.get(c).charAt(0))) {
							System.out.println("digit found");
							i = c + 1;

							while (i < everythingParse.size() && (!everythingParse.get(i).equals(")"))
									&& (!everythingParse.get(i).equals(";"))) {
								i = i + 1;

							}
							i = i + 1;
							if (i >= everythingParse.size() || (everythingParse.get(i).equals(";"))) {
								continue;
							}
							if (Character.isDigit(everythingParse.get(i).charAt(0))) {
								System.out.println(Integer.parseInt(everythingParse.get(c)));
								System.out.println(Integer.parseInt(everythingParse.get(i)));

								this.fathers.set(Integer.parseInt(everythingParse.get(c)),
										Integer.parseInt(everythingParse.get(i)));
							} else if (!everythingParse.get(c).equals(";")) {
								System.out.println(
										"error : nwk tree has not the shape : (Sister1Label,Sister2Label)fatherLabel; ");
							}
						}

					}
				} catch (IOException ex) {
					System.out.println("error :IOException");
					return;
				}
			} catch (FileNotFoundException ex) {
				System.out.println("error :FileNotFoundException");
				return;
			}

		}
	}

	/**
	 * The CustomWindow class contains the TreeJ interface and methods for user interactions
	 */
	private class CustomWindow extends StackWindow {


		/** to launch threads for the swing plugin methods and events */
		SwingWorker<Void, Void> worker = null;

		/** main panel */
		Panel all = new Panel();

		/** input image panel */
		JPanel lineagePanel = new JPanel();
		JPanel unitPanel = new JPanel();
		JPanel treePanel = new JPanel();
		JPanel outputImagePanel = new JPanel();

		/** elements of the GUI */
		JTextArea treeArea;
		JScrollPane scrollPane;
		JTextField daughter1Text;
		JTextField daughter2Text;
		JTextField unitLabel;
		JTextField note;
		JTextField unitNote;
		JTextField rootLabel;
		JTextField tagILabel;

		/** fields label */
		JLabel daughter1Label;
		JLabel daughter2Label;
		JLabel unitLabelText;
		JLabel noteText;
		JLabel unitNoteText;
		JLabel rootText;
		JLabel tagIText;

		/** buttons */
		JCheckBox assemblyCheckBox;
		JCheckBox orphanCheckBox;
		JButton linkButton;
		JButton tagButton;
		JButton reset1Button;
		JButton reset2Button;
		JButton unlinkButton;
		JButton createFileButton;
		JButton createPDFButton;
		JButton extractImageButton;
		JButton pursueFiliationButton;
		JButton extractsubImageButton;
		JButton extractTagCellsButton;

		/** text of buttons */
		private String linkButtonText = "Apply";
		private String unlinkButtonText = "Unpair";
		private String tagButtonText = "Apply";
		private String reset1ButtonText = "Reset";
		private String reset2ButtonText = "Reset";
		private String createFileButtonText = "Save";
		private String createPDFButtonText = "Export to PDF";

		private String extractImageButtonText = "Current view";
		private String extractsubImageButtonText = "Subtree ";
		private String extractTagCellsButtonText = "From tag";
		private String pursueFiliationButtonText = "Load";

		/** flag to select/deselect the assembly selection options */
		private boolean assemblySelection = false;
		/** flag to select/deselect the orphan selection options */
		private boolean orphanSelection = false;

		int nOut = 0; // count of the created output images

		CustomWindow(ImagePlus imp) {
			super(imp, new ImageCanvas(imp));

			final ImageCanvas canvas = (ImageCanvas) getCanvas();
			setTitle("TreeJ");

			treeArea = new JTextArea(20, 15);
			scrollPane = new JScrollPane(treeArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
					JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			scrollPane.setBounds(3, 3, 300, 200);
			linkButton = new JButton(linkButtonText);
			linkButton.addActionListener(listener);
			linkButton.setToolTipText("construct a tree from label 1 and label 2");
			unlinkButton = new JButton(unlinkButtonText);
			unlinkButton.addActionListener(listener);
			unlinkButton.setToolTipText("dismantle the label mother");
			tagButton = new JButton(tagButtonText);
			tagButton.addActionListener(listener);
			tagButton.setToolTipText("tag the label cell");
			createFileButton = new JButton(createFileButtonText);
			createFileButton.addActionListener(listener);
			createPDFButton = new JButton(createPDFButtonText);
			createPDFButton.addActionListener(listener);
			reset1Button = new JButton(reset1ButtonText);
			reset1Button.addActionListener(listener);
			reset1Button.setToolTipText("reset label numbers and tag");
			reset2Button = new JButton(reset2ButtonText);
			reset2Button.addActionListener(listener);
			reset2Button.setToolTipText("reset label number and tag");

			extractImageButton = new JButton(extractImageButtonText);
			extractImageButton.addActionListener(listener);
			pursueFiliationButton = new JButton(pursueFiliationButtonText);
			pursueFiliationButton.addActionListener(listener);
			extractsubImageButton = new JButton(extractsubImageButtonText);
			extractsubImageButton.addActionListener(listener);
			extractTagCellsButton = new JButton(extractTagCellsButtonText);
			extractTagCellsButton.addActionListener(listener);
			assemblyCheckBox = new JCheckBox("daughter cell selection", assemblySelection);
			assemblyCheckBox.addActionListener(listener);
			orphanCheckBox = new JCheckBox("orphan cell selection", orphanSelection);
			orphanCheckBox.addActionListener(listener);

			// daughter cell values
			daughter1Label = new JLabel("Label 1");
			// daughter1Label.setToolTipText( "label number" );
			daughter1Text = new JTextField("0", 3);
			daughter1Text.setToolTipText("label number");
			daughter2Label = new JLabel("Label 2");
			// daughter2Label.setToolTipText( "label number" );
			daughter2Text = new JTextField("0", 3);
			daughter2Text.setToolTipText("label number");
			unitLabelText = new JLabel("Label");
			// daughter1Label.setToolTipText( "label number" );
			unitLabel = new JTextField("0", 3);
			unitLabel.setToolTipText("label number");

			noteText = new JLabel("Tag");
			// noteText.setToolTipText( "" );
			note = new JTextField("", 3);
			note.setToolTipText(" Comments on the mother cell ");
			unitNoteText = new JLabel("Tag");
			// noteText.setToolTipText( "" );
			unitNote = new JTextField("", 3);
			unitNote.setToolTipText(" Comments on the mother cell ");

			rootText = new JLabel("Root");
			rootLabel = new JTextField("0", 3);
			rootLabel.setMinimumSize(rootLabel.getPreferredSize());
			rootLabel.setToolTipText("root label number");
			tagIText = new JLabel("Tag");
			tagILabel = new JTextField("", 3);
			tagILabel.setMinimumSize(tagILabel.getPreferredSize());
			tagILabel.setToolTipText("tag to extract");

			treeArea.setBorder(BorderFactory.createTitledBorder("Tree"));

			GridBagLayout treeLayout = new GridBagLayout();
			GridBagConstraints treeConstraints = new GridBagConstraints();
			treeConstraints.anchor = GridBagConstraints.WEST;
			treeConstraints.gridwidth = 1;
			treeConstraints.gridheight = 1;
			treeConstraints.gridx = 0;
			treeConstraints.gridy = 0;
			treeArea.setLayout(treeLayout);
			treeArea.setMinimumSize(treeArea.getPreferredSize());

			GridBagLayout treeFLayout = new GridBagLayout();
			GridBagConstraints treeFConstraints = new GridBagConstraints();
			treeFConstraints.fill = GridBagConstraints.BOTH;
			treeFConstraints.gridwidth = 1;
			treeFConstraints.gridheight = 1;
			treeFConstraints.gridx = 0;
			treeFConstraints.gridy = 0;
			treeFConstraints.insets = new Insets(5, 5, 6, 6);
			treePanel.setLayout(treeFLayout);
			treePanel.add(pursueFiliationButton, treeFConstraints);
			treeFConstraints.gridx++;
			treePanel.add(createFileButton, treeFConstraints);
			treeFConstraints.gridx++;
			treePanel.add(createPDFButton, treeFConstraints);
			treePanel.setMinimumSize(treePanel.getPreferredSize());
			
			
			lineagePanel.setBorder(BorderFactory.createTitledBorder("Pair cells"));
			GridBagLayout lineageLayout = new GridBagLayout();
			GridBagConstraints lineageConstraints = new GridBagConstraints();
			lineageConstraints.fill = GridBagConstraints.BOTH;
			lineageConstraints.gridwidth = 1;
			lineageConstraints.gridheight = 1;
			lineageConstraints.gridx = 0;
			lineageConstraints.gridy = 0;
			lineageConstraints.insets = new Insets(5, 5, 6, 6);
			lineagePanel.setLayout(lineageLayout);

			lineagePanel.add(daughter1Label, lineageConstraints);
			lineageConstraints.gridx++;
			lineagePanel.add(daughter1Text, lineageConstraints);
			lineageConstraints.gridx--;
			lineageConstraints.gridy++;
			lineagePanel.add(daughter2Label, lineageConstraints);
			lineageConstraints.gridx++;
			lineagePanel.add(daughter2Text, lineageConstraints);
			lineageConstraints.gridx--;
			lineageConstraints.gridy++;

			lineagePanel.add(noteText, lineageConstraints);
			lineageConstraints.gridx++;
			lineagePanel.add(note, lineageConstraints);
			lineageConstraints.gridx++;
			lineagePanel.add(tagButton, lineageConstraints);
			lineageConstraints.gridx--;
			lineageConstraints.gridx--;
			lineageConstraints.gridy++;
			lineagePanel.add(linkButton, lineageConstraints);
			lineageConstraints.gridx++;
			lineageConstraints.gridwidth = 2;
			lineagePanel.add(reset1Button, lineageConstraints);
			lineagePanel.setMinimumSize(lineagePanel.getPreferredSize());
			
			unitPanel.setBorder(BorderFactory.createTitledBorder("Unpair or tag"));
			unitPanel.setToolTipText("activate with ctrl keyboard");
			GridBagLayout unitLayout = new GridBagLayout();
			GridBagConstraints unitConstraints = new GridBagConstraints();
			unitConstraints.fill = GridBagConstraints.BOTH;
			unitConstraints.gridwidth = 1;
			unitConstraints.gridheight = 1;
			unitConstraints.gridx = 0;
			unitConstraints.gridy = 0;
			unitConstraints.insets = new Insets(5, 5, 6, 6);
			unitPanel.setLayout(unitLayout);

			unitConstraints.gridx = 0;
			unitConstraints.gridy = 0;
			unitPanel.add(unitLabelText, unitConstraints);
			unitConstraints.gridx++;
			unitPanel.add(unitLabel, unitConstraints);
			unitConstraints.gridy++;
			unitPanel.add(unlinkButton, unitConstraints);
			unitConstraints.gridx--;
			unitConstraints.gridy++;
			unitPanel.add(unitNoteText, unitConstraints);
			unitConstraints.gridx++;
			unitPanel.add(unitNote, unitConstraints);
			unitConstraints.gridy++;
			unitConstraints.gridx--;
			unitPanel.add(tagButton, unitConstraints);
			unitConstraints.gridx++;
			unitPanel.add(reset2Button, unitConstraints);
			unitPanel.setMinimumSize(unitPanel.getPreferredSize());
			
			outputImagePanel.setBorder(BorderFactory.createTitledBorder(" output image"));
			GridBagLayout outputImageLayout = new GridBagLayout();
			GridBagConstraints outputImageConstraints = new GridBagConstraints();
			outputImageConstraints.anchor = GridBagConstraints.WEST;
			outputImageConstraints.fill = GridBagConstraints.BOTH;
			outputImageConstraints.gridwidth = 1;
			outputImageConstraints.gridheight = 1;
			outputImageConstraints.gridx = 0;
			outputImageConstraints.gridy = 0;
			outputImageConstraints.insets = new Insets(5, 5, 6, 6);
			outputImagePanel.setLayout(outputImageLayout);
			outputImageConstraints.gridwidth = 1;
			outputImagePanel.add(extractImageButton, outputImageConstraints);
			outputImageConstraints.gridy++;
			outputImagePanel.add(extractsubImageButton, outputImageConstraints);

			outputImageConstraints.gridx++;
			outputImagePanel.add(rootText, outputImageConstraints);
			outputImageConstraints.gridx++;		
			outputImagePanel.add(rootLabel, outputImageConstraints);
			outputImageConstraints.gridx--;
			outputImageConstraints.gridx--;
			outputImageConstraints.gridy++;
			outputImagePanel.add(extractTagCellsButton, outputImageConstraints);
			outputImageConstraints.gridx++;
			outputImagePanel.add(tagIText, outputImageConstraints);
			outputImageConstraints.gridx++;
			outputImagePanel.add(tagILabel, outputImageConstraints);

			outputImagePanel.setMinimumSize(outputImagePanel.getPreferredSize());

			// Main panel (including parameters panel and canvas)
			GridBagLayout layout = new GridBagLayout();
			GridBagConstraints allConstraints = new GridBagConstraints();
			all.setLayout(layout);

			// put parameter panel in place
			allConstraints.anchor = GridBagConstraints.NORTHWEST;
			allConstraints.fill = GridBagConstraints.BOTH;
			allConstraints.gridwidth = 1;
			allConstraints.gridheight = 3;
			allConstraints.gridx = 0;
			allConstraints.gridy = 0;
			allConstraints.weightx = 1;
			allConstraints.weighty = 3;

			all.add(scrollPane, allConstraints);
			allConstraints.gridy = 3;
			allConstraints.gridheight = 1;
			allConstraints.fill = GridBagConstraints.HORIZONTAL;
			all.add(treePanel, allConstraints);

			// put canvas in place
			allConstraints.gridx++;
			allConstraints.gridy = 0;
			allConstraints.weightx = 1;
			allConstraints.weighty = 1;
			allConstraints.gridheight = 3;
			all.add(canvas, allConstraints);
			canvas.addMouseListener(mlAdd);
			
			allConstraints.gridy = 3;
			allConstraints.weightx = 0;
			allConstraints.weighty = 0;
			allConstraints.gridheight = 0;

			// if the input image is 3d, put the slice selectors in place
			if (null != super.sliceSelector) {
				sliceSelector.setValue(inputImage.getCurrentSlice());
				displayImage.setSlice(inputImage.getCurrentSlice());

				all.add(super.sliceSelector, allConstraints);

				if (null != super.zSelector)
					all.add(super.zSelector, allConstraints);
				if (null != super.tSelector)
					all.add(super.tSelector, allConstraints);
				if (null != super.cSelector)
					all.add(super.cSelector, allConstraints);

			}
			allConstraints.gridy = 0;
			allConstraints.gridx = 2;
			allConstraints.weightx = 1;
			allConstraints.weighty = 1;
			allConstraints.gridheight = 1;
			all.add(lineagePanel, allConstraints);
			allConstraints.gridy++;
			allConstraints.weightx = 1;
			allConstraints.weighty = 1;
			allConstraints.gridheight = 1;
			all.add(unitPanel, allConstraints);
			allConstraints.gridy++;
			allConstraints.weightx = 1;
			allConstraints.weighty = 1;
			allConstraints.gridheight = 1;
			all.add(outputImagePanel, allConstraints);

			GridBagLayout wingb = new GridBagLayout();
			GridBagConstraints winc = new GridBagConstraints();
			winc.anchor = GridBagConstraints.NORTHWEST;
			winc.fill = GridBagConstraints.HORIZONTAL;
			winc.weightx = 1;
			winc.weighty = 1;
			setLayout(wingb);
			add(all, winc);

			// Fix minimum size to the preferred size at this point
			pack();
			setMinimumSize(getPreferredSize());
			all.addComponentListener( new ComponentAdapter()
	        {
	            public void componentResized(ComponentEvent evt)
	            {
	                Dimension size = lineagePanel.getSize();
	                if (size.getWidth() == 0) 
	                {
	                	canvas.unzoom();
	                }
	            }
	        });
		}

		/**
		 * Listener for the GUI buttons
		 */
		private ActionListener listener = new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {

				final String command = e.getActionCommand();

				// listen to the buttons on separate threads not to block
				// the event dispatch thread
				worker = new SwingWorker<Void, Void>() {
					@Override
					public Void doInBackground() {
						// IJ.log(""+e.getSource()+"");
						if (e.getSource() == linkButton) {
							link(command);
						} else if (e.getSource() == unlinkButton) {
							unlink(command);
						} else if (e.getSource() == tagButton) {
							tag(command);
						} else if (e.getSource() == extractImageButton) {
							extractImage(command);
						} else if (e.getSource() == extractsubImageButton) {
							extractPartialImage(command);
						} else if (e.getSource() == extractTagCellsButton) {
							extractTag(command);
						} else if (e.getSource() == assemblyCheckBox) {

							assemblySelection = !assemblySelection;

							if (assemblySelection) {

								ic.addMouseListener(mlAdd);
							}
						} else if (e.getSource() == orphanCheckBox) {

							orphanSelection = !orphanSelection;

							if (orphanSelection) {

								ic.addMouseListener(mlAdd);
							}
						} else if (e.getSource() == reset1Button) {
							reset1(command);
						} else if (e.getSource() == reset2Button) {
							reset2(command);
						}
						return null;
					}

				};
				worker.execute();
				while (worker.isDone() == false) {
					// IJ.log("wait");
				}
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						// IJ.log(""+e.getSource()+"");
						if (e.getSource() == pursueFiliationButton) {
							pursueFiliation(command);
						} else if (e.getSource() == createFileButton) {
							createFile(command);
						} else if (e.getSource() == createPDFButton) {
							createTreePDF(command);
						}

					}

				});
			}

		};

		MouseListener mlAdd = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent eImg) {
				// IJ.log(""+eImg.getSource()+"");
				int xi = eImg.getX();
				int yi = eImg.getY();
				int ox = imp.getWindow().getCanvas().offScreenX(xi);
				int oy = imp.getWindow().getCanvas().offScreenY(yi);
				int zi = displayImage.getCurrentSlice();

				ImageStack imgStack = displayImage.getStack();

				// markersImage.setDefault16bitRange(16);
				int label = (int) imgStack.getVoxel(ox, oy, zi - 1);
				// IJ.log(""+eImg.getModifiers()+"");
				if (eImg.getModifiers() == 18 || eImg.getModifiers() == 20) {
					unitLabel.setText(Integer.toString(label));
				} else {
					if (daughter1Text.getText().equals("0")) {
						daughter1Text.setText(Integer.toString(label));
					} else {
						daughter2Text.setText(Integer.toString(label));
					}
				}

			}
		};

		private void reset1(String command) {
			daughter1Text.setText(Integer.toString(0));
			daughter2Text.setText(Integer.toString(0));
			note.setText("");
		}

		private void reset2(String command) {
			unitLabel.setText(Integer.toString(0));
			unitNote.setText("");
		}

		private void pursueFiliation(String command) {
			LogStream.redirectSystem(true);
			GenericDialog gd = new GenericDialog("previous Tree");
			gd.addCheckbox("Insert manually list of fathers", false);
			gd.addCheckbox("Load .treeV or .tree (Nwk)", false);

			gd.showDialog();
			if (gd.wasCanceled())
				return;
			java.util.Vector boxes = gd.getCheckboxes();
			Checkbox box0 = (Checkbox) boxes.get(0);
			Checkbox box1 = (Checkbox) boxes.get(1);

			if (box0.getState() == true) {
				gd.remove(box1);
				gd.addStringField("Enter the Tree as [fatherOfLabel1, fatherOfLabel2, ... fatherOfLabeln]", "", 100);
				gd.addStringField("Enter the Tree as fatherOfLabel1 fatherOfLabel2 ... fatherOfLabeln", "", 100);
				gd.showDialog();
				if (gd.wasCanceled())
					return;
				java.util.Vector str = gd.getStringFields();
				TextField str0 = (TextField) str.get(0);
				TextField str1 = (TextField) str.get(1);
				String[] everythingParse = null;
				if (str0.getText().length() != 0) {
					String strFathers = str0.getText();
					strFathers = strFathers.substring(1, strFathers.length() - 1);
					everythingParse = strFathers.split(", ");
				} else if (str1.getText().length() != 0) {
					String strFathers = str1.getText();
					everythingParse = strFathers.split("[\t| ]");
				} else {
					return;
				}

				tree.clearTree();

				for (int i = 0; i < everythingParse.length; i++) {
					tree.fathers.add(Integer.parseInt(everythingParse[i]));
					tree.notes.add("");
				}
			}

			else if (box1.getState() == true) {

				gd.remove(box0);
				// http://stackoverflow.com/questions/3548140/how-to-open-and-save-using-java
				JFileChooser fileChooser = new JFileChooser();
				if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
					File file = fileChooser.getSelectedFile();

					String arg = file.getPath();

					// http://stackoverflow.com/questions/4716503/best-way-to-read-a-text-file
					tree.clearTree();
					System.out.println(file.getPath());
					int index = file.getPath().lastIndexOf(".");
					String ext = "";

					if (index != -1) {
						ext = file.getPath().substring(index);
					} else {
						IJ.log("loading aborted : extension not recognized");
						return;
					}
					if (ext.equals(".treeV")) {
						tree.readTreeV(arg);

					} else if (ext.equals(".nwk")) {
						tree.readNwk(arg);

					}
				} else {
					IJ.log("loading aborted : extension not recognized");
					return;
				}
			} else {
				return;
			}

			System.out.println(tree.fathers);
			System.out.println(tree.notes);
			int value = 0;
			ImageStack imgStack = displayImage.getStack();
			int width = imgStack.getWidth();
			int height = imgStack.getHeight();
			int nSlices = imgStack.getSize();
			for (int z = 0; z < nSlices; z++) {
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						value = tree.fathers.get((int) imgStack.getVoxel(x, y, z));
						if (value != 0) {
							while (tree.fathers.get(value) != 0) {
								value = tree.fathers.get(value);
							}
							imgStack.setVoxel(x, y, z, value);
						}
					}
				}
			}
			System.out.println(tree.fathers);
			System.out.println(tree.notes);

			extractTree();
			displayImage.updateAndDraw();
			tree.nextFather = tree.listNode.size();
		}


		private void extractTree() {
			for (int i = 0; i < tree.listNode.size(); i++) {
				tree.listNode.get(i).Note = tree.notes.get(i);
			}
			// listNode = new ArrayList<Node>();
			// create node
			for (int i = tree.listNode.size(); i < tree.fathers.size(); i++) {
				tree.listNode.add(new Node(i));
				tree.listNode.get(i).Note = tree.notes.get(i);
			}

			// link node
			for (int i = 0; i < tree.fathers.size(); i++) {

				if (tree.fathers.get(i) != 0) {
					tree.listNode.get(tree.fathers.get(i)).insertOneDaughter(tree.listNode.get(i));

				}
			}

			tree.writeStrTree();
			treeArea.setText(tree.str.toString());
			IJ.log("higher label number : " + (tree.listNode.size() - 1) + "");
		}

		private void extractPartialImage(String command) {
			int root = Integer.parseInt(rootLabel.getText());
			String fileName = "subTreeFrom" + root + "-" + inputImage.getTitle();

			java.util.List<Integer> daughterlabels = new ArrayList<Integer>();
			int f = 0;
			for (int i = 0; i < tree.fathers.size(); i++) {
				if (tree.listNode.get(i).left == null) {
					f = i;
					while (f != 0) {
						if (f == root) {
							daughterlabels.add(i);
						}
						f = tree.fathers.get(f);
					}
				}
			}
			createImageFromCellsList(daughterlabels, fileName);
		}

		private void extractTag(String command) {
			String nt = tagILabel.getText();
			if (nt.equals("")) {
				IJ.log("warning : no specified Tag in the \"Tag\" Field ");
			}
			String fileName = nt + "cells-" + inputImage.getTitle();
			java.util.List<Integer> daughterlabels = new ArrayList<Integer>();
			int f = 0;
			for (int i = 0; i < tree.fathers.size(); i++) {
				if (tree.listNode.get(i).left == null) {
					f = i;
					while (f != 0) {
						if (nt.equals(tree.listNode.get(f).Note)) {
							daughterlabels.add(i);
						}
						f = tree.fathers.get(f);
					}
				}
			}
			createImageFromCellsList(daughterlabels, fileName);
			tagILabel.setText("");
		}

		private void createImageFromCellsList(java.util.List<Integer> daughterlabels, String name) {
			int width = inputStackCopy.getWidth();
			int height = inputStackCopy.getHeight();
			int nSlices = inputStackCopy.getSize();
			int bitDepth = inputStackCopy.getBitDepth();
			ImagePlus out = IJ.createImage(name, width, height, nSlices, bitDepth);
			ImageStack imgStack = out.getStack();

			int value = 0;
			for (int z = 0; z < nSlices; z++) {
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						value = (int) inputImage.getImageStack().getVoxel(x, y, z);
						if (daughterlabels.contains(value)) {
							imgStack.setVoxel(x, y, z, value);
						}
					}
				}
			}
			// out.setTitle( "subTreeFrom"+root+"-"+ inputImage.getTitle() );
			out.setCalibration(inputImage.getCalibration());
			out.getProcessor().setColorModel(displayImage.getProcessor().getCurrentColorModel());
			out.setDisplayRange(0, 255);
			out.setSlice(win.getImagePlus().getSlice());
			out.show();
		}

		private void extractImage(String command) {
			ImagePlus out = displayImage.duplicate();

			out.setTitle("outLineage" + nOut + "-" + inputImage.getTitle());
			nOut = nOut + 1;
			out.setCalibration(inputImage.getCalibration());
			out.show();
			out.setSlice(win.getImagePlus().getSlice());
		}

		private void tag(String command) {
			LogStream.redirectSystem(true);
			// ij.io.LogStream.redirectSystem(true);
			if (command.equals(tagButtonText)) {

				int label1 = Integer.parseInt(unitLabel.getText());
				tree.notes.set(label1, unitNote.getText());
				tree.listNode.get(label1).Note = unitNote.getText();
				// display tree
				tree.writeStrTree();
				treeArea.setText(tree.str.toString());
				unitLabel.setText("0");
				unitNote.setText("");
			}
		}

		private void link(String command) {
			LogStream.redirectSystem(true);
			// IJ.log("is thread dispatched :
			// "+javax.swing.SwingUtilities.isEventDispatchThread()+"\n");
			// ij.io.LogStream.redirectSystem(true);
			if (command.equals(linkButtonText)) {
				// help from https://stackoverflow.com/questions/38234433/imagej-convert-an-existing-imageprocessor-picture-rgb-into-an-8-bit-grayvalue
				if ((tree.nextFather+1) >= Math.pow(2, displayImage.getBitDepth())) {
					IJ.log("not enought depth to add mother labels : image depth extended");
					ImageConverter ic = new ImageConverter(displayImage);
					if (displayImage.getBitDepth() == 8) {
						ic.convertToGray16();
					} else if (displayImage.getBitDepth() == 16) {
						ic.convertToGray32();
					} else {
						IJ.log("WARNING : probably not enought depth to add mother labels");
					}
					displayImage.updateAndDraw();
				}
				
				ImageStack imgStack = displayImage.getStack();
				int width = imgStack.getWidth();
				int height = imgStack.getHeight();
				int nSlices = imgStack.getSize();

				// gather labels to link
				int label1 = Integer.parseInt(daughter1Text.getText());
				int label2 = Integer.parseInt(daughter2Text.getText());

				// check if values are not background and not similar
				if (label1 != 0 && label2 != 0 && label1 != label2) {
					// set their link to the new father
					tree.fathers.set(label1, tree.nextFather);
					tree.fathers.set(label2, tree.nextFather);

					// update the image
					for (int z = 0; z < nSlices; z++) {
						for (int y = 0; y < height; y++) {
							for (int x = 0; x < width; x++) {

								if (imgStack.getVoxel(x, y, z) == label1 || imgStack.getVoxel(x, y, z) == label2) {
									imgStack.setVoxel(x, y, z, tree.nextFather);
								}

							}
						}
					}

					displayImage.updateAndDraw();
					// create the new cell (the father cell)
					tree.fathers.add(0);
					tree.notes.add(note.getText());
					tree.listNode.add(new Node(tree.nextFather, tree.listNode.get(label1), tree.listNode.get(label2)));
					tree.listNode.get(tree.listNode.size() - 1).Note = note.getText();

					// reset the fields
					updateFields();

					// display tree
					tree.writeStrTree();
					treeArea.setText(tree.str.toString());
					
					tree.nextFather++;

				}
				System.out.println(tree.fathers);
				System.out.println(tree.notes);

			}
		}

		private void unlink(String command) {
			LogStream.redirectSystem(true);
			// ij.io.LogStream.redirectSystem(true);

			if (command.equals(unlinkButtonText)) {

				int label1 = Integer.parseInt(unitLabel.getText());
				if (label1 != 0) {
					tree.unlinkFromMother(label1);
					updateImageFromTree();
					displayImage.updateAndDraw();
					// display tree
					tree.writeStrTree();
					treeArea.setText(tree.str.toString());
					updateFields();
				}
				System.out.println(tree.fathers);
				System.out.println(tree.notes);
			}
		}

		private void updateImageFromTree() {
			int value = 0;
			ImageStack imgStack = displayImage.getStack();
			int width = imgStack.getWidth();
			int height = imgStack.getHeight();
			int nSlices = imgStack.getSize();
			for (int z = 0; z < nSlices; z++) {
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						value = (int) inputImage.getImageStack().getVoxel(x, y, z);
						if (value != 0) {
							while (tree.fathers.get(value) != 0) {
								value = tree.fathers.get(value);
							}
							imgStack.setVoxel(x, y, z, value);
						}
					}
				}
			}
		}

		private void updateDisplayedTree() {
			tree.writeStrTree();
			treeArea.setText(tree.str.toString());
		}

		private void updateFields() {
			daughter1Text.setText("0");
			daughter2Text.setText("0");
			note.setText("");
			unitLabel.setText("0");

		}

		private void createFile(String command) {
			LogStream.redirectSystem(true);

			// ij.io.LogStream.redirectSystem(true);
			if (command.equals(createFileButtonText)) {

				IJ.log("saving Enter");
				String title = inputImage.getTitle();
				int index = title.lastIndexOf(".");
				if (index != -1) {
					title = title.substring(0, index);
				}
				// DirectoryChooser dir = new DirectoryChooser("select a directory");
				JFileChooser fc = new JFileChooser();
				fc.setDialogTitle("Save your tree");
				// fc.setCurrentDirectory(new File("~/" + title +".treeV")) ;
				fc.setSelectedFile(new File("~/" + title + ".treeV"));

				// add here possible file extensions
				fc.addChoosableFileFilter(new FileNameExtensionFilter("Newick", "nwk"));
				fc.addChoosableFileFilter(new FileNameExtensionFilter("treeV", "treeV"));
				fc.addPropertyChangeListener(JFileChooser.FILE_FILTER_CHANGED_PROPERTY, new PropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent e) {
						FileNameExtensionFilter n = (FileNameExtensionFilter) e.getNewValue();
						String[] ext = n.getExtensions();

						JFileChooser fc = (JFileChooser) e.getSource();

						FileChooserUI ui = fc.getUI();
						String f = ((BasicFileChooserUI) ui).getFileName();

						int indexExt = f.lastIndexOf(".");
						if (indexExt != -1) {
							f = f.substring(0, indexExt);
						}

						// adapt the file name regarding the chosen extension
						if (ext[0] == "treeV") {
							fc.setSelectedFile(new File(f + ".treeV"));
						} else if (ext[0] == "nwk") {
							fc.setSelectedFile(new File(f + ".nwk"));
						} else {
							fc.setSelectedFile(new File(f));
						}

					}
				});
				
				File file = null;
				switch (fc.showSaveDialog(this)) {
				case JFileChooser.APPROVE_OPTION:
					file = fc.getSelectedFile();
					if (file.getName() == null) {
						IJ.log("saving aborted");
						return;
					}
					break;
				case JFileChooser.CANCEL_OPTION:
					return;
				}

				try {
					file.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}

				String ext = "";
				index = file.getName().lastIndexOf(".");
				if (index != -1) {
					ext = file.getName().substring(index);

					if (ext.equals(".treeV")) {
						tree.writeToTreeV(file);
					} else if (ext.equals(".nwk")) {
						tree.writeTreeToNwk(file);
					}
				}
			}
		}

		private void createTreePDF(String command) {
			LogStream.redirectSystem(true);

			// ij.io.LogStream.redirectSystem(true);
			if (command.equals(createPDFButtonText)) {

				IJ.log("saving Enter");
				DirectoryChooser dir = new DirectoryChooser("select a directory");
				dir.setDefaultDirectory("~");

				String dirName = dir.getDirectory();
				IJ.log("" + dirName + "");
				if (dirName == null) {
					IJ.log("saving aborted");
					return;
				}
				// create image name
				String title = inputImage.getTitle();
				int index = title.lastIndexOf(".");
				if (index != -1) {
					title = title.substring(0, index);
					tree.writeToPDF(dirName + title + ".pdf");
				}
			}
		}

		/**
		 * Overwrite windowClosing to display the input image after closing the GUI and
		 * shut down the worker
		 */
		@Override
		public void windowClosing(WindowEvent e) {
			super.windowClosing(e);

			if (null != inputImage) {
				if (null != displayImage)
					inputImage.setSlice(displayImage.getCurrentSlice());
				
				// display input image
				inputImage.getWindow().setVisible(true);
			}

			// remove listeners
			// borderButton.removeActionListener( listener );

			if (null != displayImage) {
				// displayImage.close();
				displayImage = null;
			}
			// shut down w service
			if (worker != null) {
				worker.cancel(true);
			}
		}

	}

	/**
	 * code to lunch the interface and initialise the tree structure
	 */
	
	/** main GUI window */
	private CustomWindow win;
	/** original input image */
	ImagePlus inputImage = null;

	/** copy of original input image stack */
	ImageStack inputStackCopy = null;

	/** image to be displayed in the GUI */
	ImagePlus displayImage = null;

	Tree tree = null;

	@Override
	public void run(String arg0) {
		ij.io.LogStream.redirectSystem(true);
		if (IJ.getVersion().compareTo("1.48a") < 0) {
			IJ.error("filiation tool", "ERROR: detected ImageJ version " + IJ.getVersion()
					+ ".\nfiliation tool requires version 1.48a or superior, please update ImageJ!");
			return;
		}
		IJ.log("step 1");

		// get current image
		if (null == WindowManager.getCurrentImage()) {
			inputImage = IJ.openImage();

			if (null == inputImage)
				return; // user cancelled open dialog
			inputImage.show();
		} else
			inputImage = WindowManager.getCurrentImage();

		if (inputImage.getType() == ImagePlus.COLOR_256 || inputImage.getType() == ImagePlus.COLOR_RGB) {
			IJ.error("filiation Tool",
					"This plugin only works on grayscale images.\nPlease convert it to 8, 16 or 32-bit.");
			return;
		}
		IJ.log("step 2");
		
		if (inputImage.getLocalCalibration().isSigned16Bit()) {
			// calibrate values when it's calibrated images
			ImagePlus inImg = inputImage.duplicate();
			Calibration C = inputImage.getCalibration();
			inputStackCopy = inImg.getImageStack().duplicate();
			int sizeX = inputStackCopy.getWidth();
			int sizeY = inputStackCopy.getHeight();
			int sizeZ = inputStackCopy.getSize();
			for (int z = 0; z < sizeZ; z++) {
				IJ.showProgress(z, sizeZ);
				for (int y = 0; y < sizeY; y++)
					for (int x = 0; x < sizeX; x++)
						inputStackCopy.setVoxel(x, y, z, C.getCValue(inputStackCopy.getVoxel(x, y, z)));
			}
		}
		else {
			inputStackCopy = inputImage.getImageStack().duplicate();
		}		
		displayImage = new ImagePlus(inputImage.getTitle(), inputStackCopy);
		displayImage.setTitle("TreeJ");
		displayImage.setSlice(inputImage.getSlice());


		
		// correct Fiji error when the slices are read as frames
		if (displayImage.isHyperStack() == false && displayImage.getNSlices() == 1) {
			// correct stack by setting number of frames as slices
			displayImage.setDimensions(1, displayImage.getNFrames(), 1);
		}
		
		// hide input image (to avoid accidental closing)
		inputImage.getWindow().setVisible(false);
		
		tree = new Tree();
		tree.initialize(inputStackCopy);

		displayImage.setDisplayRange(-1, tree.max*2); // not optimal : to change
		displayImage.updateAndDraw();

		// Build GUI
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				win = new CustomWindow(displayImage);
				win.pack();

			}
		});

	}
}
