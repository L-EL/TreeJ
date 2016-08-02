import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.process.ImageProcessor;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.StackWindow;
import ij.io.LogStream;
import ij.io.DirectoryChooser;
import ij.plugin.PlugIn;

import javax.swing.BorderFactory;
import javax.swing.JTextField;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JButton;
import javax.swing.plaf.basic.BasicFileChooserUI;
import javax.swing.plaf.FileChooserUI;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.JScrollPane;
import javax.swing.JFileChooser;

import java.awt.image.ColorModel;
import java.awt.Dimension;
import java.awt.image.IndexColorModel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
//import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.event.WindowEvent;
import java.awt.Checkbox;
import java.awt.TextField;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Iterator;
import java.util.Random;
import java.util.TreeSet;

import java.lang.System;

import java.io.PrintWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.io.OutputStreamWriter;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.io.BufferedReader;
import java.io.FileReader;

import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.DocumentException;
import ij.process.LUT;

public class TreeJ_ implements PlugIn {

// ------------- class for display the tree
// adapted from http://stackoverflow.com/questions/4965335/how-to-print-binary-tree-diagram/8948691#8948691
// by Elise 

	public static class Node {
    Integer value;
    String Note ;
    Node left, right;
	public Node()
	{
		
	}
	public Node(int cellLabel)
		{
			value = cellLabel;
			Note = "";
		}
	public Node(int motherLabel, Node daughter1, Node daughter2) {
			value = motherLabel;
			left = daughter1;
			 right = daughter2;
			 Note = "";
		}
	public void insertOneDaughter(Node Daughter) {
        
       if (left == null) 
       {
                left = Daughter;
        } 
        else  if (right == null) 
        {
                right = Daughter;
        }
        else
        {
        	IJ.log("erreur : more than 2 daughter cells ?! ");
        }
	}
        
    public void insertToTree(int v) {
        if (value == null) {
            value = v;
            return;
        }
       if (left == null) {
                left = new Node();
            
            left.insertToTree(v);
        } 
        else  if (right == null) {
                right = new Node();
            
            right.insertToTree(v);
        }
        
    }

    public void printTree(StringWriter out)  {    	 
        if (right != null) {
            right.printTree(out, true, "");
        }        
        printNodeValue(out);
        if (left != null) {
            left.printTree(out, false, "");
        }         
    }
    private void printNodeValue(StringWriter out)  {
        if (value == null) {
            out.write("<null>");
        } else {						
        	if (Note.isEmpty())
        	{
        		out.write(Integer.toString(value));
        	}
        	else
        	{
        		out.write(Note+"("+Integer.toString(value)+")");
        	}
        }
        out.write('\n');
    }
    // use string and not stringbuffer on purpose as we need to change the indent at each recursion
    private void printTree(StringWriter out, boolean isRight, String indent)  {
        if (right != null) {
            right.printTree(out, true, indent + (isRight ? "        " : " |      "));
        }
       
        out.write(indent);
        if (isRight) {
            out.write(" /");
        } else {
            out.write(" \\");
        }
        out.write("----- ");
        printNodeValue(out);
        if (left != null) {
            left.printTree(out, false, indent + (isRight ? " |      " : "        "));
        }
    }

}

	
// ------------------ ENd Node class

/** main GUI window */
	private CustomWindow win;

	/** executor service to launch threads for the plugin methods and events */
	final ExecutorService exec = Executors.newFixedThreadPool(1);
	
	/** main panel */
	Panel all = new Panel();

	/** original input image */
	ImagePlus inputImage = null;
	
	/** copy of original input image stack */
	ImageStack inputStackCopy = null;

	/** image to be displayed in the GUI */
	ImagePlus displayImage = null;
/** input image panel */
	JPanel inputImagePanel = new JPanel();
	JPanel lineagePanel = new JPanel();
	JPanel unitPanel = new JPanel();
	JPanel treePanel = new JPanel();
	JPanel outputImagePanel = new JPanel();
	
	JTextArea treeArea;
	JScrollPane scrollPane;
	//JPanel filiation;
	JTextField daughter1Text;
	JTextField daughter2Text;
	JTextField unitLabel;
	JTextField note;
	JTextField unitNote;
	JTextField rootLabel;
	JTextField tagILabel;
	/** thresold label */
	JLabel daughter1Label;
	JLabel daughter2Label;
	JLabel unitLabelText;
	JLabel noteText;
	JLabel unitNoteText;
	JLabel rootText;
	JLabel tagIText;
	
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
	JSeparator x1 ;
	JSeparator x2 ;
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

	//Lineage used
	java.util.List<Integer> fathers;
	java.util.List<String> notes;
	java.util.List<Integer> labels;
	java.util.List<Node> listNode;
	int nextFather;
	StringWriter str;
	byte[] rLUT;
    byte[] gLUT;
    byte[] bLUT;
    int nOut = 0;
	
	private class CustomWindow extends StackWindow
	{
		CustomWindow( ImagePlus imp )
		{
			super(imp, new ImageCanvas(imp));
			
			final ImageCanvas canvas = (ImageCanvas) getCanvas();
			setTitle( "TreeJ" );

			treeArea = new JTextArea(20,15);
			scrollPane = new JScrollPane(treeArea,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS); 
                scrollPane.setBounds(3, 3, 300, 200);
			linkButton = new JButton( linkButtonText );
			linkButton.addActionListener( listener );
			linkButton.setToolTipText( "construct a tree from label 1 and label 2" );
			unlinkButton = new JButton( unlinkButtonText );
			unlinkButton.addActionListener( listener );
			unlinkButton.setToolTipText( "dismantle the label1 mother" );
			tagButton = new JButton( tagButtonText );
			tagButton.addActionListener( listener );
			tagButton.setToolTipText( "tag the label1 cell" );
			createFileButton= new JButton( createFileButtonText );
			createFileButton.addActionListener( listener );
			createPDFButton= new JButton( createPDFButtonText );
			createPDFButton.addActionListener( listener );
			reset1Button = new JButton( reset1ButtonText );
			reset1Button.addActionListener( listener );
			reset1Button.setToolTipText( "reset label numbers and tag" );
			reset2Button = new JButton( reset2ButtonText );
			reset2Button.addActionListener( listener );
			reset2Button.setToolTipText( "reset label number and tag" );
			
			extractImageButton =  new JButton(extractImageButtonText);
			extractImageButton.addActionListener( listener );
			pursueFiliationButton =  new JButton(pursueFiliationButtonText);
			pursueFiliationButton.addActionListener( listener );
			extractsubImageButton =  new JButton(extractsubImageButtonText);
			extractsubImageButton.addActionListener( listener );
			extractTagCellsButton =  new JButton(extractTagCellsButtonText);
			extractTagCellsButton.addActionListener( listener );
			assemblyCheckBox =new JCheckBox( "daughter cell selection", assemblySelection );
			assemblyCheckBox.addActionListener( listener );
			orphanCheckBox = new JCheckBox( "orphan cell selection", orphanSelection );
			orphanCheckBox.addActionListener( listener );

			// daughter cell values
         			daughter1Label = new JLabel( "Label 1" );
         			//daughter1Label.setToolTipText( "label number" );
         			daughter1Text = new JTextField( "0", 3 );
         			daughter1Text.setToolTipText( "label number" );
         			daughter2Label = new JLabel( "Label 2" );
         			//daughter2Label.setToolTipText( "label number" );
         			daughter2Text = new JTextField( "0", 3 );
         			daughter2Text.setToolTipText( "label number" );
         			unitLabelText = new JLabel( "Label" );
         			//daughter1Label.setToolTipText( "label number" );
         			unitLabel = new JTextField( "0", 3 );
         			unitLabel.setToolTipText( "label number" );
         			
         			noteText = new JLabel( "Tag" );
         			//noteText.setToolTipText( "" );
         			note = new JTextField( "", 3 );
         			note.setToolTipText( " Comments on the mother cell " );
         			unitNoteText = new JLabel( "Tag" );
         			//noteText.setToolTipText( "" );
         			unitNote = new JTextField( "", 3 );
         			unitNote.setToolTipText( " Comments on the mother cell " );
         			
         			rootText = new JLabel( "Root" );
         			rootLabel = new JTextField( "0", 3 );
         			rootLabel.setToolTipText( "root label number" );
					tagIText = new JLabel( "Tag" );
					tagILabel = new JTextField( "", 3 );
         			tagILabel.setToolTipText( "tag to extract" );
         			
			treeArea.setBorder( BorderFactory.createTitledBorder( "Tree" ) );
			
			GridBagLayout treeLayout = new GridBagLayout();
			GridBagConstraints treeConstraints = new GridBagConstraints();
			treeConstraints.anchor = GridBagConstraints.WEST;
			treeConstraints.gridwidth = 1;
			treeConstraints.gridheight = 1;
			treeConstraints.gridx = 0;
			treeConstraints.gridy = 0;
			treeArea.setLayout( treeLayout );
			
			//treePanel.setBorder( BorderFactory.createTitledBorder( "tree file tools" ) );
			GridBagLayout treeFLayout = new GridBagLayout();
			GridBagConstraints treeFConstraints = new GridBagConstraints();
			//lineageConstraints.anchor = GridBagConstraints.WEST;
			treeFConstraints.fill = GridBagConstraints.VERTICAL;
			treeFConstraints.gridwidth = 1;
			treeFConstraints.gridheight = 1;
			treeFConstraints.gridx = 0;
			treeFConstraints.gridy = 0;
			treeFConstraints.insets = new Insets(5, 5, 6, 6);
			treePanel.setLayout( treeFLayout );
			treePanel.add(pursueFiliationButton, treeFConstraints );
			treeFConstraints.gridx++;
			treePanel.add(createFileButton, treeFConstraints );
			treeFConstraints.gridx++;
			treePanel.add( createPDFButton, treeFConstraints );

			
			lineagePanel.setBorder( BorderFactory.createTitledBorder( "Pair cells" ) );
			GridBagLayout lineageLayout = new GridBagLayout();
			GridBagConstraints lineageConstraints = new GridBagConstraints();
			//lineageConstraints.anchor = GridBagConstraints.WEST;
			lineageConstraints.fill = GridBagConstraints.BOTH;
			lineageConstraints.gridwidth = 1;
			lineageConstraints.gridheight = 1;
			lineageConstraints.gridx = 0;
			lineageConstraints.gridy = 0;
			lineageConstraints.insets = new Insets(5, 5, 6, 6);
			lineagePanel.setLayout( lineageLayout );
						

			lineageConstraints.gridy++;


			lineageConstraints.gridwidth = 1;
			lineageConstraints.gridy++;

	lineagePanel.add(daughter1Label, lineageConstraints );
	lineageConstraints.gridx++;	
	lineagePanel.add(daughter1Text, lineageConstraints );
	
			//lineageConstraints.gridx++;
			//lineagePanel.add( unlinkButton, lineageConstraints );		
	//lineageConstraints.gridx--;
	lineageConstraints.gridx--;
	lineageConstraints.gridy++;
	lineagePanel.add(daughter2Label, lineageConstraints );
	lineageConstraints.gridx++;
	lineagePanel.add(daughter2Text, lineageConstraints );
	lineageConstraints.gridx--;		
			lineageConstraints.gridy++;

			
			lineagePanel.add(noteText, lineageConstraints );
			lineageConstraints.gridx++;
			lineagePanel.add(note, lineageConstraints );
			lineageConstraints.gridx++;
			lineagePanel.add( tagButton, lineageConstraints );
			lineageConstraints.gridx--;
			lineageConstraints.gridx--;
			lineageConstraints.gridy++;
			lineagePanel.add( linkButton, lineageConstraints );	
			lineageConstraints.gridx++;
			lineageConstraints.gridwidth = 2;
			lineagePanel.add( reset1Button, lineageConstraints );	


			
			unitPanel.setBorder( BorderFactory.createTitledBorder( "Unpair or tag" ) );
			unitPanel.setToolTipText( "activate with ctrl keyboard" );
			GridBagLayout unitLayout = new GridBagLayout();
			GridBagConstraints unitConstraints = new GridBagConstraints();
			//lineageConstraints.anchor = GridBagConstraints.WEST;
			unitConstraints.fill = GridBagConstraints.BOTH;
			unitConstraints.gridwidth = 1;
			unitConstraints.gridheight = 1;
			unitConstraints.gridx = 0;
			unitConstraints.gridy = 0;
			unitConstraints.insets = new Insets(5, 5, 6, 6);
			unitPanel.setLayout( unitLayout );
			
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
			
			outputImagePanel.setBorder( BorderFactory.createTitledBorder( " output image" ) );
			GridBagLayout outputImageLayout = new GridBagLayout();
			GridBagConstraints outputImageConstraints = new GridBagConstraints();
			outputImageConstraints.anchor = GridBagConstraints.WEST;
			outputImageConstraints.fill = GridBagConstraints.BOTH;
			outputImageConstraints.gridwidth = 1;
			outputImageConstraints.gridheight = 1;
			outputImageConstraints.gridx = 0;
			outputImageConstraints.gridy = 0;
			outputImageConstraints.insets = new Insets(5, 5, 6, 6);
			outputImagePanel.setLayout( outputImageLayout );
			outputImageConstraints.gridwidth = 1;
			outputImagePanel.add( extractImageButton, outputImageConstraints );
			outputImageConstraints.gridy++;
			outputImageConstraints.gridwidth = 1;
			outputImagePanel.add( extractsubImageButton, outputImageConstraints );

			outputImageConstraints.gridx++;
			outputImageConstraints.gridwidth = 1;
			outputImagePanel.add(rootText, outputImageConstraints );						
			outputImageConstraints.gridx++;
			outputImageConstraints.gridheight = 1;
			outputImagePanel.add(rootLabel, outputImageConstraints );
			outputImageConstraints.gridx--;
			outputImageConstraints.gridx--;
			outputImageConstraints.gridy++;
			outputImageConstraints.gridwidth = 1;
			outputImagePanel.add( extractTagCellsButton, outputImageConstraints );
			outputImageConstraints.gridx++;
			outputImageConstraints.gridwidth = 1;
			outputImagePanel.add(tagIText, outputImageConstraints );						
			outputImageConstraints.gridx++;
			outputImagePanel.add(tagILabel, outputImageConstraints );
			
			

			
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
			//allConstraints.gridheight = 2;
			allConstraints.weightx = 1;
			allConstraints.weighty = 3;

			all.add( scrollPane, allConstraints );
			allConstraints.gridy = 3 ; 
			allConstraints.gridheight = 1;
			allConstraints.fill = GridBagConstraints.HORIZONTAL;
			all.add( treePanel, allConstraints );

			// put canvas in place
			allConstraints.gridx++;
			allConstraints.gridy= 0;			
			allConstraints.weightx = 1;
			allConstraints.weighty = 1;
			allConstraints.gridheight = 3;
			
			all.add( canvas, allConstraints );
			canvas.addMouseListener(mlAdd);
			//all.addKeyListener(this);
			
			allConstraints.gridy = 3;
			allConstraints.weightx = 0;
			allConstraints.weighty = 0;
			allConstraints.gridheight = 0;
			//allConstraints.fill = GridBagConstraints.HORIZONTAL;
			// if the input image is 3d, put the
			// slice selectors in place
			if( null != super.sliceSelector )
			{
				sliceSelector.setValue( inputImage.getCurrentSlice() );
				displayImage.setSlice( inputImage.getCurrentSlice() );
				
				all.add( super.sliceSelector, allConstraints );

				if( null != super.zSelector )
					all.add( super.zSelector, allConstraints );
				if( null != super.tSelector )
					all.add( super.tSelector, allConstraints );
				if( null != super.cSelector )
					all.add( super.cSelector, allConstraints );

			}
			allConstraints.gridy = 0;
			allConstraints.gridx = 2;
			allConstraints.weightx = 1;
			allConstraints.weighty = 1;
			allConstraints.gridheight = 1;
			all.add( lineagePanel, allConstraints );
			allConstraints.gridy++;
			allConstraints.weightx = 1;
			allConstraints.weighty = 1;
			allConstraints.gridheight = 1;
			all.add( unitPanel, allConstraints );
			allConstraints.gridy++;
			allConstraints.weightx = 1;
			allConstraints.weighty = 1;
			allConstraints.gridheight = 1;
			all.add(outputImagePanel, allConstraints );

			GridBagLayout wingb = new GridBagLayout();
			GridBagConstraints winc = new GridBagConstraints();
			winc.anchor = GridBagConstraints.NORTHWEST;
			winc.fill = GridBagConstraints.HORIZONTAL;
			winc.weightx = 1;
			winc.weighty = 1;
			setLayout( wingb );
			add( all, winc );

			// Fix minimum size to the preferred size at this point
			pack();
			setMinimumSize( getPreferredSize() );
		}


		/**
		 * Listener for the GUI buttons
		 */
		private ActionListener listener = new ActionListener() {

			@Override
			public void actionPerformed( final ActionEvent e ) 
			{

				final String command = e.getActionCommand();

				// listen to the buttons on separate threads not to block
				// the event dispatch thread
				exec.submit(new Runnable() {

					public void run()
					{
						//IJ.log(""+e.getSource()+"");
						
						
						if( e.getSource() == linkButton )
						{
							link( command );						
						}	
						else if( e.getSource() == unlinkButton )
						{
							unlink( command );						
						}	
						else if( e.getSource() == tagButton )
						{
							tag( command );						
						}	
						else if( e.getSource() == pursueFiliationButton)
						{
							pursueFiliation( command);
						}
						else if( e.getSource() == extractImageButton	)
						{
							extractImage( command);			
						}
						else if( e.getSource() == extractsubImageButton	)
						{
							extractPartialImage( command);			
						}
						else if( e.getSource() == extractTagCellsButton	)
						{
							extractTag( command);			
						}
						else if( e.getSource() == createFileButton )
						{
							//createTreeVFile(command);
							createFile(command);
						}
						else if( e.getSource() == createPDFButton )
						{
							createTreePDF(command);
						}
						
						else if( e.getSource() == assemblyCheckBox )
						{
							
							assemblySelection = !assemblySelection;
							 
							
							if (assemblySelection)
							{
								
								ic.addMouseListener(mlAdd);
							}
						}
					else if( e.getSource() == orphanCheckBox )
						{
							
							orphanSelection = !orphanSelection;
							 
							
							if (orphanSelection)
							{
								
								ic.addMouseListener(mlAdd);
							}
						}
						else if( e.getSource() == reset1Button	)
						{
							reset1( command);			
						}
						else if( e.getSource() == reset2Button	)
						{
							reset2( command);			
						}

					}
					
				});
			}

		};

		MouseListener mlAdd = new MouseAdapter() {
				    @Override
				    public void mouseClicked(MouseEvent eImg) {
				    	//IJ.log(""+eImg.getSource()+"");
				    	int xi = eImg.getX();
				    	int yi = eImg.getY();
				    	int ox = imp.getWindow().getCanvas().offScreenX(xi);
						int oy = imp.getWindow().getCanvas().offScreenY(yi);
				    	int zi = displayImage.getCurrentSlice();
				    	
				    	
				    	ImageStack imgStack = displayImage.getStack();
				    	
				    	//markersImage.setDefault16bitRange(16);
				    	int label = (int) imgStack.getVoxel(ox,oy,zi-1);
						//IJ.log(""+daughter1Text.getText().getClass()+"");
						//IJ.log(""+eImg.getModifiers()+"");
						 if(eImg.getModifiers() == 18 || eImg.getModifiers() == 20){
						 	unitLabel.setText( Integer.toString(label) );
						 }
						 else
						 {
					    	if (daughter1Text.getText().equals("0"))
					    	{
					    		daughter1Text.setText( Integer.toString(label) );
					    	}
					    	else
					    	{
					    		daughter2Text.setText( Integer.toString(label) );
					    	}
						 }
				    	
				    }
				};

		private void reset1( String command)
			{			
				daughter1Text.setText( Integer.toString(0) );
				daughter2Text.setText( Integer.toString(0) );
				note.setText("");
			}
			private void reset2( String command)
			{			
				unitLabel.setText( Integer.toString(0) );
				unitNote.setText("");
			}

			private void resetTree()
			{
				for (int i = 0 ; i < listNode.size(); i++) 
	            {
	            	listNode.get(i).left = null ;
					listNode.get(i).right = null ;
	            	listNode.get(i).Note = "";
	            }
			}
		private void pursueFiliation( String command)
			{			
			LogStream.redirectSystem(true);
			GenericDialog gd = new GenericDialog("previous Tree");
			gd. addCheckbox("Insert manually list of fathers", false);			
			gd.addCheckbox("Load .treeV or .nwk", false);
			
			gd.showDialog();
			if (gd.wasCanceled())
			    return ;
			java.util.Vector boxes = gd.getCheckboxes();
			Checkbox box0=(Checkbox)boxes.get(0);
			Checkbox box1=(Checkbox)boxes.get(1);
			//System.out.println(box0.getState());
			
			if (box0.getState() == true)
			{
				gd.addStringField("Enter the Tree as [fatherOfLabel1, fatherOfLabel2, ... fatherOfLabeln]", "", 100);
				gd.addStringField("Enter the Tree as fatherOfLabel1 fatherOfLabel2 ... fatherOfLabeln", "", 100);
				gd.showDialog();
				if (gd.wasCanceled())
			    	return ;
				java.util.Vector str = gd.getStringFields();
				TextField str0=(TextField)str.get(0);
				TextField str1=(TextField)str.get(1);
				if (str0.getText().length() != 0)
				{
					String strFathers = str0.getText();
					strFathers = strFathers.substring(1,strFathers.length()-1);
					String[] everythingParse = strFathers.split(", ");					
					fathers.clear();
					notes.clear();		
					resetTree();			
					//fathers = new ArrayList<Integer>(everythingParse.length +1);
					
		            for (int i = 0; i < everythingParse.length; i++) 
		            {
		            	
		            	fathers.add(Integer.parseInt(everythingParse[i]));
		            	notes.add("");
		            }
					/*		            // create node
		            for (int i = listNode.size(); i < fathers.size(); i++) 
		            {
		            	listNode.add(new Node(i));
		            	listNode.get(i);
		            }*/
				}
				else if (str1.getText().length() != 0)
				{
					String strFathers = str1.getText();
					String[] everythingParse = strFathers.split("[\t| ]");	
					fathers.clear();
					notes.clear();		
					resetTree();			
					//fathers = new ArrayList<Integer>(everythingParse.length);
					
           			for (int i = 0; i < everythingParse.length; i++) 
          			  {
            	
        			    	fathers.add(Integer.parseInt(everythingParse[i]));
        			    	notes.add("");
        			   }
		        	/*		  // notes.add("");
		        			               // create node
		            for (int i = listNode.size(); i < fathers.size(); i++) 
		            {
		            	listNode.add(new Node(i));
		            	listNode.get(i);
		            }*/
            
				}

			}
				
			
			else if (box1.getState() == true)
			{
				//http://stackoverflow.com/questions/3548140/how-to-open-and-save-using-java
			
				JFileChooser fileChooser = new JFileChooser();
				if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) 
				{
 					File file = fileChooser.getSelectedFile();
  					//http://stackoverflow.com/questions/4716503/best-way-to-read-a-text-file
	     			fathers.clear();
			      	notes.clear();	
			      	resetTree();			      	
  					System.out.println(file.getPath());
  					int index = file.getPath().lastIndexOf( "." );
  					String ext = "";
  					
					if( index != -1 )
					{
						ext = file.getPath().substring( index );
					}
  					if (ext.equals( ".treeV"))
  					{
 					try
 						 {
  								BufferedReader br = new BufferedReader(new FileReader(file.getPath()));
  								try {
   									StringBuilder sb = new StringBuilder();
        						    String line = br.readLine();
        						    int lineNumber = 0 ;
      						     	while (line != null) 
      						     	{
      						        	//sb.append(line);
    						            //sb.append(java.lang.System.lineSeparator());
    						            
    						            if(lineNumber==0)
    						            {
    						            	String everything = line.toString();
		     						      	String[] everythingParse = everything.split("[\t| ]");
		     						      	System.out.println(everythingParse);

		    						        //fathers = new ArrayList<Integer>(everythingParse.length);
		    						        fathers.add(0); // <--- because the treeV array mean [P(1)	P(2) ...P(n)] where P() mean the father  and here father is [P(index)] (index start from zero)
		    						        notes.add("");
		     						        for (int i = 0; i < everythingParse.length; i++) 
		    						            {
			     						           	fathers.add(Integer.parseInt(everythingParse[i]));
			     						           	notes.add("");
			     						        }            
    						            }
    						            else if (lineNumber ==1)
    						            {
    						            	String everything = line.toString();
		    						        System.out.println(everything);
		    						        String tmp = "";
		    						        int i = 1;
		    						        while (everything.length() !=0)
		    						        {
		    						        	if (i>	  fathers.size())
		    						        	{
		    						        		System.out.println("error : more tag than cell ");
		    						        	}
		    						        	if ( everything.charAt(0) != '\t')
		    						            	{
		    						            		tmp = tmp.concat(String.valueOf(everything.charAt(0)));
		    						            		
		    						            	}
		    						          	else
		    						            	{
		    						            		notes.set(i,tmp);
		    						            		tmp = "";
		    						            		i = i+1;
		    						            	}
		    						            	everything = everything.substring(1);
		    						        }
		    						        /* // alternative way
											for (int i = 1; i < fathers.size(); i++) 
		    						            {
		    						            	
		    						            	if ( everything.charAt(0) != '\t')
		    						            	{
		    						            		notes.set(i,String.valueOf(everything.charAt(0)));
		    						            		everything = everything.substring(1);
		    						            		listNode.get(i).Note = String.valueOf(everything.charAt(0));
		    						            	}
		    						            	everything = everything.substring(1);
			     						           	
		     						          	}*/
    						            }
    						            else{
    						            	 System.out.println("error : more than two lines in the treeV file ");
    						            }
    						            line = br.readLine();
    						            lineNumber = lineNumber +1;
    						        }
   						             br.close();
        							//notes.add("");
 						       } 
 						       catch (IOException ex) 
 						      	 {
 						      	 	System.out.println("error :IOException" );
   						         return;
  						     	 }
 						   }
						   catch (FileNotFoundException ex)
						    {
						    	System.out.println("error :FileNotFoundException" );

 						     	return ;
 						     }
  					
				}
				else if (ext.equals( ".nwk"))
				{
					try
					 {
						BufferedReader br = new BufferedReader(new FileReader(file.getPath()));
						try {
							StringBuilder sb = new StringBuilder();
						    String line = br.readLine();
						    // parse string :
						    java.util.List<String> everythingParse = new ArrayList<String>();
						    String tmp= "";
						    int fmax = 0;
						    while (line.length() != 0)
						    {
						    	if (Character.isDigit(line.charAt(0)))
						    	{
						    		while(Character.isDigit(line.charAt(0)))
						    		{
							    		tmp = tmp.concat(String.valueOf(line.charAt(0)));
							    		line = line.substring(1);
						    		}
						    		everythingParse.add(tmp);
						    		
						    		if (Integer.parseInt(tmp)>fmax)
						    		{
						    			fmax = Integer.parseInt(tmp);
						    		}
						    		tmp = "";
						    	}
						    	else
						    	{
						    		everythingParse.add(String.valueOf(line.charAt(0)));
						    		line = line.substring(1);
						    	}
						    	
						    }
						    System.out.println(everythingParse);
						    // create nul father list : 
						    for (int i = 0; i <= fmax; i++) 
					            {
					            	fathers.add(0);
					            	notes.add("");
					            }
					            // search the father for each label :
					            int i = 0;
					         for (int c = 0; c< everythingParse.size(); c++)
					         {
					         	if (Character.isDigit(everythingParse.get(c).charAt(0)))
					         	{
					         		System.out.println("digit found");
					         		i = c+1;
					         		
					         		while (i < everythingParse.size() &&(!everythingParse.get(i).equals(")")) &&(!everythingParse.get(i).equals(";")) )
					         		{
					         			i = i+1;
					         			
					         		}
					         		i = i+1;
					         		if (i >= everythingParse.size() || (everythingParse.get(i).equals(";")))
					         		{
					         			continue;
					         		}
					         		if (Character.isDigit(everythingParse.get(i).charAt(0)))
					         		{
					         			System.out.println(Integer.parseInt(everythingParse.get(c)));
					         			System.out.println(Integer.parseInt(everythingParse.get(i)));

					         			fathers.set(Integer.parseInt(everythingParse.get(c)),Integer.parseInt(everythingParse.get(i)));
					         		}
					         		else if (!everythingParse.get(c).equals(";" ))
					         		{
					         			System.out.println("error : nwk tree has not the shape : (Sister1Label,Sister2Label)fatherLabel; " );
					         		}
					         	}
					         	
					         }
				   		   } 
			      		catch (IOException ex) 
				      	 {
				      	 	System.out.println("error :IOException" );
				        	return;
				     	 }
					}
				   catch (FileNotFoundException ex)
				    {
				    	System.out.println("error :FileNotFoundException" );
					    return ;
					 }
					 
				}
				}
 				else
 				{
 					return;
 				}
 				
			}

			int value = 0;
					ImageStack imgStack = displayImage.getStack();
				    	int width = imgStack.getWidth();
						int height = imgStack.getHeight();
						int nSlices = imgStack.getSize();
						int bitDepth = imgStack.getBitDepth() ;
						for(int z = 0; z < nSlices; z++){  	 	 	
					  	 	 	for (int y = 0; y <height; y++){
					  	 	 		for (int x = 0; x < width; x++){
					  	 	 			value = fathers.get((int)imgStack.getVoxel(x, y, z));
					  	 	 			if(value != 0 )
					  	 	 			{
					  	 	 				while( fathers.get(value) != 0)
					  	 	 				{
					  	 	 					value = fathers.get(value);
					  	 	 				}
					  	 	 			imgStack.setVoxel(x, y, z,value);
					  	 	 			}
					  	 	 		}
					  	 	 	}
						}
						
    					System.out.println(fathers);
    					System.out.println(notes);

						extractTree();
						displayImage.updateAndDraw();
						nextFather = listNode.size();
		}

		
		private void extractTree()
		{
			for (int i = 0; i < listNode.size(); i++) 
            {
            	listNode.get(i).Note = notes.get(i);
            }
			//listNode = new ArrayList<Node>();
            // create node
            for (int i = listNode.size(); i < fathers.size(); i++) 
            {
            	listNode.add(new Node(i));
            	listNode.get(i).Note = notes.get(i);
            }
            
            //link node
            for (int i = 0; i < fathers.size(); i++) 
            {
            	
            	if( fathers.get(i) != 0)
            		{
            			listNode.get(fathers.get(i)).insertOneDaughter(listNode.get(i));
            			
            		}
            }
            str.getBuffer().setLength(0);
            // display tree(s?)
            for (int i = 0; i < fathers.size(); i++) 
            {
            	
            	if( fathers.get(i) == 0 && listNode.get(i).left != null)
            	{            		
            		listNode.get(i).printTree(str);
            	}
            }
            treeArea.insert(str.toString(),0);
            IJ.log("higher label number : "+(listNode.size()-1)+"");
		}

		
		private void extractPartialImage( String  command)
			{
				int root = Integer.parseInt( rootLabel.getText() );
			int width = inputStackCopy.getWidth();
			int height = inputStackCopy.getHeight();
			int nSlices = inputStackCopy.getSize();
			int bitDepth = inputStackCopy.getBitDepth() ;
			ImagePlus out = IJ.createImage("subTreeFrom"+root+"-"+ inputImage.getTitle(), width, height, nSlices, bitDepth) ;
			int value = 0;
			ImageStack imgStack = out.getStack();
			java.util.List<Integer> daughterlabels = new ArrayList<Integer>();	
			int f = 0;
			 for (int i = 0; i < fathers.size(); i++) 
            {
            	if(  listNode.get(i).left == null)
            	{            
            		f = i;
            		while( f != 0)
 	 				{
 	 					if (f == root)
 	 					{
 	 						daughterlabels.add(i);
 	 					}
 	 					f = fathers.get(f);
 	 				}
            	}
            }
						for(int z = 0; z < nSlices; z++){  	 	 	
					  	 	 	for (int y = 0; y <height; y++){
					  	 	 		for (int x = 0; x < width; x++){
					  	 	 			value = (int)inputImage.getImageStack().getVoxel(x, y, z);
					  	 	 			if( daughterlabels.contains(value) )
					  	 	 			{
					  	 	 				imgStack.setVoxel(x, y, z,value);
					  	 	 			}
					  	 	 		}
					  	 	 	}
						}
			//out.setTitle( "subTreeFrom"+root+"-"+ inputImage.getTitle() );
			out.setCalibration(inputImage.getCalibration());
			out.getProcessor().setColorModel( displayImage.getProcessor().getCurrentColorModel() );
			out.setDisplayRange( 0, 255);
			out.setSlice( win.getImagePlus().getSlice() );
			out.show();
			
			}
			
		private void extractTag( String  command)
			{
			String nt =  tagILabel.getText() ;
			if (nt.equals(""))
			{
				IJ.log("warning : no specified Tag in the \"Tag\" Field ");
			}
			int width = inputStackCopy.getWidth();
			int height = inputStackCopy.getHeight();
			int nSlices = inputStackCopy.getSize();
			int bitDepth = inputStackCopy.getBitDepth() ;
			ImagePlus out = IJ.createImage(nt+"cells-"+ inputImage.getTitle(), width, height, nSlices, bitDepth) ;
			int value = 0;
			ImageStack imgStack = out.getStack();
			java.util.List<Integer> daughterlabels = new ArrayList<Integer>();	
			int f = 0;
			 for (int i = 0; i < fathers.size(); i++) 
            {
            	if(  listNode.get(i).left == null)
            	{            
            		f = i;
            		while( f != 0)
 	 				{
 	 					if (nt.equals(listNode.get(f).Note) )
 	 					{
 	 						daughterlabels.add(i);
 	 					}
 	 					f = fathers.get(f);
 	 				}
            	}
            }
						for(int z = 0; z < nSlices; z++){  	 	 	
					  	 	 	for (int y = 0; y <height; y++){
					  	 	 		for (int x = 0; x < width; x++){
					  	 	 			value = (int)inputImage.getImageStack().getVoxel(x, y, z);
					  	 	 			if( daughterlabels.contains(value) )
					  	 	 			{
					  	 	 				imgStack.setVoxel(x, y, z,value);
					  	 	 			}
					  	 	 		}
					  	 	 	}
						}
			//out.setTitle( "subTreeFrom"+root+"-"+ inputImage.getTitle() );
			out.setCalibration(inputImage.getCalibration());
			out.getProcessor().setColorModel( displayImage.getProcessor().getCurrentColorModel() );
			out.setDisplayRange( 0, 255);
			out.setSlice( win.getImagePlus().getSlice() );
			out.show();
			tagILabel.setText( "");
			}
 		private void extractImage( String  command)
			{
			ImagePlus out = displayImage.duplicate();
			
			out.setTitle( "outLineage"+nOut+"-"+ inputImage.getTitle() );
			nOut = nOut +1;
			out.setCalibration(inputImage.getCalibration());
			out.show();
			out.setSlice( win.getImagePlus().getSlice() );
			}

			private void tag( String command ) 
				{
					LogStream.redirectSystem(true);
					
					//ij.io.LogStream.redirectSystem(true); 
					if ( command.equals( tagButtonText) ) 
					{
				
						int label1 = Integer.parseInt( unitLabel.getText() );
						notes.set(label1, unitNote.getText());
						listNode.get(label1).Note = unitNote.getText();
						str.getBuffer().setLength(0);
						for (int i = 0; i < fathers.size(); i++) 
			            {
			            	
			            	if( fathers.get(i) == 0 && listNode.get(i).left != null)
			            	{            		
			            		listNode.get(i).printTree(str);
			            	}
			            }
			            treeArea.setText("");
						treeArea.insert(str.toString(),0);
			            unitLabel.setText( "0" );
						
						unitNote.setText( "");
					}
				}
		private void link( String command ) 
				{
					LogStream.redirectSystem(true);
					
					//ij.io.LogStream.redirectSystem(true); 
					if ( command.equals( linkButtonText) ) 
					{
				IJ.log("jusque ici -2 "+listNode.size()+" "+fathers.size()+" "+notes.size()+"");
						int label1 = Integer.parseInt( daughter1Text.getText() );
						int label2 = Integer.parseInt( daughter2Text.getText() );
						if (label1!=0 && label1!=label2)
						{
						IJ.log("jusque ici -1 "+label1+" "+label2+" "+nextFather+"");
						
						fathers.set(label1, nextFather);
						fathers.set(label2, nextFather);
						
						IJ.log("jusque ici 0");
						ImageStack imgStack = displayImage.getStack();
				    	int width = imgStack.getWidth();
						int height = imgStack.getHeight();
						int nSlices = imgStack.getSize();
						int bitDepth = imgStack.getBitDepth() ;
						for(int z = 0; z < nSlices; z++){  	 	 	
					  	 	 	for (int y = 0; y <height; y++){
					  	 	 		for (int x = 0; x < width; x++){
					  	 	 			if(imgStack.getVoxel(x, y, z) == label1 || imgStack.getVoxel(x, y, z) == label2)
					  	 	 			{
					  	 	 			imgStack.setVoxel(x, y, z, nextFather);
					  	 	 			}
					  	 	 		}
					  	 	 	}
						}
						IJ.log("jusque ici 1");
/**
			//byte[] lut = inputImage.getProcessor().getLut().getBytes();
			ColorModel cm = imp.getProcessor().getCurrentColorModel();
        //if (cm instanceof IndexColorModel) {
            IndexColorModel m = (IndexColorModel)cm;
            int mapSize = m.getMapSize();
            byte[] rLUT = new byte[mapSize];
            byte[] gLUT = new byte[mapSize];
            byte[] bLUT = new byte[mapSize];
            m.getReds(rLUT);
            m.getGreens(gLUT);
            m.getBlues(bLUT);
            IJ.log("r : "+(rLUT[nextFather]&0xff) +"");IJ.log("g : "+ (gLUT[nextFather]&0xff) +"");IJ.log("b : "+(bLUT[nextFather]&0xff) +"");
            //displayImage.setDisplayRange( 0, nextFather );
            displayImage.resetDisplayRange();
            //imgStack.update(displayImage.getProcessor());
            //displayImage.getProcessor().setColorModel(cm);
			System.out.println(m);
			IJ.log("bytes : "+mapSize+"");**/
			//displayImage.setDisplayRange(displayImage.getDisplayRangeMin(), displayImage.getDisplayRangeMax() +1.0 );
			
			displayImage.updateAndDraw();
				fathers.add(0);		
				notes.add(note.getText());
			//str.flush(); 
			str.getBuffer().setLength(0);
			IJ.log("jusque ici 2");
			listNode.add(new Node(nextFather,listNode.get(label1),listNode.get(label2)));
			listNode.get(listNode.size()-1).Note = note.getText();

			 for (int i = 0; i < fathers.size(); i++) 
            {
            	
            	if( fathers.get(i) == 0 && listNode.get(i).left != null)
            	{            		
            		listNode.get(i).printTree(str);
            	}
            }
            
			//listNode.get(nextFather).printTree(str);
			treeArea.setText("");
			treeArea.insert(str.toString(),0);

			nextFather++;			
			IJ.log("next label father : "+nextFather+"");
			//IJ.log(""+fathers.size()+"");
			daughter1Text.setText( "0" );
			daughter2Text.setText( "0");
			note.setText( "");
			}
			System.out.println(fathers);
			System.out.println(notes);
			}
				}

	private void unlink( String command ) 
				{
					LogStream.redirectSystem(true);
					
					//ij.io.LogStream.redirectSystem(true); 
					if ( command.equals( unlinkButtonText) ) 
					{
				
						int label1 = Integer.parseInt( unitLabel.getText() );
						if (label1 !=0 )
						{
							do 
							{
								listNode.get(label1).left = null;
								listNode.get(label1).right = null;
								listNode.get(label1).Note = "";
								notes.set(label1,"") ;

								for (int i = 0; i < fathers.size(); i++) 
					            {
					            	if( fathers.get(i) == label1 )
					            	{
					            		fathers.set(i,0);
					            	}
					            }
					            label1 = fathers.get(label1);
							}while ( label1 != 0);
						
						 
						int value = 0;
						ImageStack imgStack = displayImage.getStack();
				    	int width = imgStack.getWidth();
						int height = imgStack.getHeight();
						int nSlices = imgStack.getSize();
						int bitDepth = imgStack.getBitDepth() ;
						for(int z = 0; z < nSlices; z++){  	 	 	
					  	 	 	for (int y = 0; y <height; y++){
					  	 	 		for (int x = 0; x < width; x++){
					  	 	 			value = (int)inputImage.getImageStack().getVoxel(x, y, z);
					  	 	 			if(value != 0 )
					  	 	 			{
					  	 	 				while( fathers.get(value) != 0)
					  	 	 				{
					  	 	 					value = fathers.get(value);
					  	 	 				}
					  	 	 			imgStack.setVoxel(x, y, z,value);
					  	 	 			}
					  	 	 		}
					  	 	 	}
						}
						

			//displayImage.setDisplayRange(displayImage.getDisplayRangeMin(), displayImage.getDisplayRangeMax() +1.0 );
			
			displayImage.updateAndDraw();
			//str.flush(); 
			str.getBuffer().setLength(0);
			 for (int i = 0; i < fathers.size(); i++) 
            {
            	
            	if( fathers.get(i) == 0 && listNode.get(i).left != null)
            	{            		
            		listNode.get(i).printTree(str);
            	}
            }
            
			//listNode.get(nextFather).printTree(str);
			treeArea.setText("");
			treeArea.insert(str.toString(),0);

			IJ.log("next label father : "+nextFather+"");
			//IJ.log(""+fathers.size()+"");
			unitLabel.setText( "0" );
			
			//note.setText( "");
			}
			System.out.println(fathers);
			System.out.println(notes);
			}
				}
				
			/**private void actualizeTree( String command ) 
			{
				treeArea.setText("");
				String text="";
				for (int i=0;i<fathers.length;i++)
				{
					if (fathers[i] == 0)
					{
						text=text+Integer.toString(i)+"\t";
					}
				}
				
			}**/
		private void createTreeVFile( String command ) 
				{
					LogStream.redirectSystem(true);
					
					//ij.io.LogStream.redirectSystem(true); 
					if ( command.equals( createFileButtonText) ) 
			{
				
			IJ.log("saving Enter");
			DirectoryChooser dir = new DirectoryChooser("select a directory");
			dir.setDefaultDirectory("~");
			
			String dirName = dir.getDirectory();
			IJ.log(""+dirName+"");
			if (dirName == null )
			{
				IJ.log("saving aborted");
				return;
			}
			String title = inputImage.getTitle();
			String ext = "";
			int index = title.lastIndexOf( "." );
			if( index != -1 )
			{
				ext = title.substring( index );
				title = title.substring( 0, index );				
			}
			//IJ.log("title: "+title +".treeV");
			File file = new File(dirName + title +".treeV");
			try {
	            file.createNewFile();
	            } 
	            catch (IOException e)
	            {
	            e.printStackTrace();
	            }
			
			try
			{				
				PrintWriter writer = new PrintWriter(file);
				for(int i=1; i<fathers.size();i++)
				{
					writer.print(Integer.toString(fathers.get(i)));
					writer.print("\t");
				}
				
				writer.close();
				IJ.log("file save as : "+dirName + title +".treeV");
			}
			catch (FileNotFoundException ex)
			{
				IJ.log("error");
				System.out.println("file error in filiationTool");
				IJ.error( "file error in filiationTool", "FileNotFoundException" );				
			}


          
			}
				}

	private void createFile( String command ) 
				{
					LogStream.redirectSystem(true);
					
					//ij.io.LogStream.redirectSystem(true); 
					if ( command.equals( createFileButtonText) ) 
			{
				
			IJ.log("saving Enter");
			String title = inputImage.getTitle();
			int index = title.lastIndexOf( "." );
			if( index != -1 )
			{
				title = title.substring( 0, index );				
			}
			//DirectoryChooser dir = new DirectoryChooser("select a directory");
			JFileChooser fc = new JFileChooser();
			fc.setDialogTitle("Save your tree");
			//fc.setCurrentDirectory(new File("~/" +  title +".treeV")) ;
			fc.setSelectedFile(new File("~/" +  title +".treeV"));
			//fc.addChoosableFileFilter(new FileNameExtensionFilter("Nexus", "nexus"));
			
			fc.addChoosableFileFilter(new FileNameExtensionFilter("Newick", "nwk"));
			fc.addChoosableFileFilter(new FileNameExtensionFilter("treeV", "treeV"));
			fc.addPropertyChangeListener(JFileChooser.FILE_FILTER_CHANGED_PROPERTY, new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
            	FileNameExtensionFilter  n = (FileNameExtensionFilter)e.getNewValue();
	            String[] ext = n.getExtensions();
	           
	            Object fcn = e.getSource() ;
	            
	            
	            JFileChooser fc = (JFileChooser) e.getSource() ;
	                      
	            FileChooserUI ui = fc.getUI();
	            String f = ((BasicFileChooserUI)ui).getFileName();
	            
	            int indexExt = f.lastIndexOf( "." );
	            if( indexExt != -1 )
				{				
					f= f.substring( 0, indexExt );				
				}
	           
	            if (ext[0] == "treeV")
	            {
	            	fc.setSelectedFile(new File(f +".treeV"));
	            }
	            else if (ext[0] == "nwk")
	            {
	            	fc.setSelectedFile(new File(f +".nwk"));
	            }
	            else if (ext[0] == "nexus")
	            {
	            	fc.setSelectedFile(new File(f +".nexus"));
	            }
	            else 
	            {
	            	fc.setSelectedFile(new File(f));
	            }
	            
            }});
			//fc.showSaveDialog()
			File file= null;
			switch(fc.showSaveDialog(this))
			{
				case JFileChooser.APPROVE_OPTION :
  					file = fc.getSelectedFile();
  					if (file.getName() == null )
						{
							IJ.log("saving aborted");
							return;
						}
  					break;
  				case JFileChooser.CANCEL_OPTION :
	  				return ;
			}
			
			try {
	            file.createNewFile();
	            } 
	            catch (IOException e)
	            {
	            e.printStackTrace();
	            }
				
				
			String ext = "";
			index = file.getName().lastIndexOf( "." );
			if( index != -1 )
			{
				ext = file.getName().substring( index );
							
			}
			if (ext.equals(".treeV"))
			{
				try
				{				
					PrintWriter writer = new PrintWriter(file);
					int i ;
					for(i=1; i<fathers.size();i++)
					{
						writer.print(Integer.toString(fathers.get(i)));
						writer.print("\t");
					}
					
					writer.print("\n");
					for(i=1; i<fathers.size();i++)
					{
						writer.print(notes.get(i));
						writer.print("\t");
					}
					
					writer.close();
					IJ.log("file save as : "+file.getName());
				}
				catch (FileNotFoundException ex)
				{
					IJ.log("error");
					System.out.println("file error in filiationTool");
					IJ.error( "file error in filiationTool", "FileNotFoundException" );				
				}
			}
			else if (ext.equals( ".nwk"))
			{
				try
				{
				
					PrintWriter writer = new PrintWriter(file);
					for(int i=1; i<fathers.size();i++)
					{
						if( fathers.get(i) == 0 && listNode.get(i).left != null)
		            	{       
		            		IJ.log("root" + i);     		
		            		String a = 	buildStrTree(listNode.get(i)) +";\n";
		            		writer.print(a);
		            	}
					}
					writer.close();
					IJ.log("file save as : "+file.getName());
				}
					catch (FileNotFoundException ex)
				{
					IJ.log("error");
					System.out.println("file error in filiationTool");
					IJ.error( "file error in filiationTool", "FileNotFoundException" );				
				}
			}

          
			}
				}

		private String buildStrTree(Node t) 
		{
			String strTree = "";
		
		if (t.left == null && t.right == null) 
		{
			IJ.log(Integer.toString(t.value));
			strTree = Integer.toString(t.value);
		}
		else 
		{
			strTree = "(" + buildStrTree(t.left) +","+ buildStrTree(t.right)+")"+Integer.toString(t.value);
		}
		return strTree;
		}
		
		private void createTreePDF( String command ) 
				{
					LogStream.redirectSystem(true);
					
					//ij.io.LogStream.redirectSystem(true); 
					if ( command.equals( createPDFButtonText) ) 
			{
				
			IJ.log("saving Enter");
			DirectoryChooser dir = new DirectoryChooser("select a directory");
			dir.setDefaultDirectory("~");
			
			String dirName = dir.getDirectory();
			IJ.log(""+dirName+"");
			if (dirName == null )
			{
				IJ.log("saving aborted");
				return;
			}
			 str.getBuffer().setLength(0);
			String title = inputImage.getTitle();
			String ext = "";
			int index = title.lastIndexOf( "." );
			if( index != -1 )
			{
				ext = title.substring( index );
				title = title.substring( 0, index );				
			}

			 for (int i = 0; i < fathers.size(); i++) 
            {
            	
            	if( fathers.get(i) == 0 && listNode.get(i).left != null)
            	{            		
            		listNode.get(i).printTree(str);
            	}
            }

			Document document = new Document();
		  try {
          PdfWriter.getInstance(document, new FileOutputStream(dirName + title +".pdf"));
			}
          catch (DocumentException e)
	            {
	            e.printStackTrace();
	            }
	      catch (FileNotFoundException e)
	         	{
	            e.printStackTrace();
	            }
	           
          document.open();
          try {
          document.add(new Paragraph(str.toString()));
          }
          catch (DocumentException e)
	            {
	            e.printStackTrace();
	            }
          document.close();
          



          
			}
				}
				

          
	/**
		 * Overwrite windowClosing to display the input image after closing 
		 * the GUI and shut down the executor service
		 */
		@Override
		public void windowClosing( WindowEvent e ) 
		{							
			super.windowClosing( e );

			if( null != inputImage )
			{
				if( null != displayImage )
					inputImage.setSlice( displayImage.getCurrentSlice() );
				
				// display input image
				inputImage.getWindow().setVisible( true );
			}

			// remove listeners
			//borderButton.removeActionListener( listener );
			
			
			if( null != displayImage )
			{
				//displayImage.close();
				displayImage = null;
			}
			// shut down executor service
			exec.shutdownNow();
		}

	}
	
		private final static byte[][] createRandomLut() {
		Random rand =  new Random();
		// create map
		byte[][] map = new byte[256][3];
		
		// cast elements
		for (int i = 0; i < 256; i++) {
			IJ.log(""+rand.nextInt(255  + 1)+"");
			map[i][0] = (byte) rand.nextInt(255  + 1);
			map[i][1] = (byte) rand.nextInt(255  + 1);
			map[i][2] = (byte) rand.nextInt(255  + 1);
			
		}
		return  map;
	}
	@Override
	public void run(String arg0) 
	{
		ij.io.LogStream.redirectSystem(true); 
		if ( IJ.getVersion().compareTo("1.48a") < 0 )
		{
			IJ.error( "filiation tool", "ERROR: detected ImageJ version " + IJ.getVersion()  
					+ ".\nfiliation tool requires version 1.48a or superior, please update ImageJ!" );
			return;
		}

		// get current image
		if (null == WindowManager.getCurrentImage())
		{
			inputImage = IJ.openImage();
			
			if (null == inputImage) return; // user canceled open dialog
			inputImage.show();
		}
		else
			inputImage = WindowManager.getCurrentImage();

		if( inputImage.getType() == ImagePlus.COLOR_256 || 
				inputImage.getType() == ImagePlus.COLOR_RGB )
		{
			IJ.error( "filiation Tool", "This plugin only works on grayscale images.\nPlease convert it to 8, 16 or 32-bit." );
			return;
		}

		inputStackCopy = inputImage.getImageStack().duplicate();
		displayImage = new ImagePlus( inputImage.getTitle(), 
				inputStackCopy );
		displayImage.setTitle("TreeJ");
		displayImage.setSlice( inputImage.getSlice() );

		// hide input image (to avoid accidental closing)
		inputImage.getWindow().setVisible( false );


		// correct Fiji error when the slices are read as frames
		if ( displayImage.isHyperStack() == false && displayImage.getNSlices() == 1 )
		{
			// correct stack by setting number of frames as slices
			displayImage.setDimensions( 1, displayImage.getNFrames(), 1 );
		}

		//labels = new ArrayList<Integer>();
		int sizeX = inputStackCopy.getWidth();
        int sizeY = inputStackCopy.getHeight();
        int sizeZ = inputStackCopy.getSize();

// from David Legland ijpb-plugins/src/main/java/inra/ijpb/morphology/LabelImages.java
         TreeSet<Integer> labelsTree = new TreeSet<Integer> ();
        
        // iterate on image pixels
        for (int z = 0; z < sizeZ; z++) {
        	IJ.showProgress(z, sizeZ);
        	for (int y = 0; y < sizeY; y++) 
        		for (int x = 0; x < sizeX; x++) 
        			labelsTree.add((int) inputStackCopy.getVoxel(x, y, z));
        }
        IJ.showProgress(1);
        
        /** remove 0 if it exists
        if (labels.contains(0))
            labels.remove(0);
        **/
        // convert to array of integers
        int max = 0 ;
        int[] array = new int[labelsTree.size()];
        fathers = new ArrayList<Integer>(labelsTree.size());
        notes = new ArrayList<String>(labelsTree.size()+1);
        Iterator<Integer> iterator = labelsTree.iterator();
        
        for (int i = 0; i < labelsTree.size(); i++) 
            {
            	array[i] = iterator.next();
            	
            	if (array[i]>max)
            	{
            		max=array[i];
            	}
            }
            listNode = new ArrayList<Node>();
            
            for (int i = 0; i <= max; i++) 
            {
            	listNode.add(new Node(i));
            	fathers.add(0);
            	notes.add("");
            }
            //notes.add("");
            IJ.log("nb of label "+labelsTree.size()+"");
            IJ.log("label number max :  "+max+"");
            IJ.log("nb of label (with zero (as background?)) :  "+fathers.size()+"");
            
       labels = new ArrayList<Integer>(labelsTree);       
		nextFather = max+1;
		str = new StringWriter();
		
/**
		//color managment
		ColorModel cm = displayImage.getProcessor().getCurrentColorModel();
        //if (cm instanceof IndexColorModel) {
            IndexColorModel m = (IndexColorModel)cm;
            int mapSize = m.getMapSize();
            byte[] rLUT = new byte[mapSize];
            byte[] gLUT = new byte[mapSize];
            byte[] bLUT = new byte[mapSize];
            m.getReds(rLUT);
            m.getGreens(gLUT);
            m.getBlues(bLUT);

		System.arraycopy(rLUT, 0, rLUT, 0, max+1);
		System.arraycopy(gLUT, 0, gLUT, 0, max+1);
		System.arraycopy(bLUT, 0, bLUT, 0, max+1);
		IJ.log("len :  "+rLUT.length+"");
		cm = new IndexColorModel(8,max+1,rLUT,gLUT,bLUT);
		ColorModel cm = ColorModel(16);
		inputImage.getProcessor().setColorModel(cm);
		inputImage.getImageStack().setColorModel(cm);**/
		//ColorModel cm = ColorModel(16);	
		//byte[][] colorMap = createRandomLut();
		//ColorModel cm = ColorMaps.createColorModel(colorMap, Color.BLACK);
		//displayImage.getProcessor().setColorModel(cm);
		IJ.log("max display : "+displayImage.getDisplayRangeMax() +"");
		IJ.log("min display : "+displayImage.getDisplayRangeMin() +"");
		displayImage.setDisplayRange( -1, 600); //0 255 //-1 600 ); //  65 536
		//inputImage.setLut(new LUT(8,max+1,rLUT,gLUT,bLUT));
		//inputImage.getProcessor().setThreshold((double)0,(double)( max+1),ImageProcessor.NO_LUT_UPDATE);
		// IJ.log("LutUpdateMode:  "+inputImage.getProcessor().getLutUpdateMode()+"");
		//inputImage.resetDisplayRange();
		displayImage.updateAndDraw();
//IJ.log("len :  "+rLUT.length+"");
		
		// Build GUI
		SwingUtilities.invokeLater(
				new Runnable() {
					public void run() {
						win = new CustomWindow( displayImage );
						win.pack();
					}
				});

	}
}
