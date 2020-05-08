/*******************************************************************************
 * #%L
 * Armedia JMeter Gherkin Plugin
 * %%
 * Copyright (C) 2020 Armedia, LLC
 * %%
 * This file is part of the Armedia JMeter Gherkin Plugin software.
 * 
 * If the software was purchased under a paid Armedia JMeter Gherkin Plugin license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 * 
 * Armedia JMeter Gherkin Plugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Armedia JMeter Gherkin Plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Armedia JMeter Gherkin Plugin. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.commons.jmeter.plugins.gherkin.sampler.gui;

import java.awt.BorderLayout;
import java.awt.Event;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.Document;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import org.apache.jmeter.gui.util.JSyntaxTextArea;
import org.apache.jmeter.gui.util.JTextScrollPane;
import org.apache.jmeter.samplers.gui.AbstractSamplerGui;
import org.apache.jmeter.testelement.TestElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.jmeter.plugins.gherkin.sampler.GherkinSampler;

public class GherkinSamplerGui extends AbstractSamplerGui {
	private static final long serialVersionUID = 1L;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final JFileChooser chooser = new JFileChooser();
	private JTextField parameters;
	private JTextField storyFile;
	private JSyntaxTextArea story;

	public GherkinSamplerGui() {
		setLayout(new BorderLayout(0, 5));
		setBorder(makeBorder());

		Box box = Box.createVerticalBox();
		box.add(makeTitlePanel());
		box.add(makeParametersPanel());
		box.add(makeFileSelectorPanel());
		add(box, BorderLayout.NORTH);
		add(createStoryPanel(), BorderLayout.CENTER);
	}

	private JPanel makeParametersPanel() {
		this.parameters = new JTextField();
		final JLabel label = new JLabel("Parameters:");
		label.setLabelFor(this.parameters);

		final JPanel panel = new JPanel(new BorderLayout(5, 0));
		panel.add(label, BorderLayout.WEST);
		panel.add(this.parameters, BorderLayout.CENTER);

		panel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED),
			"Parameters passed to the story (exposed as ${args[1]}, ${args[2]}, etc...)", TitledBorder.LEFT,
			TitledBorder.TOP));
		return panel;
	}

	private JPanel makeFileSelectorPanel() {
		this.storyFile = new JTextField();
		final JLabel label = new JLabel("File Name:");
		label.setLabelFor(this.storyFile);

		final JPanel panel = new JPanel(new BorderLayout(5, 0));
		panel.add(label, BorderLayout.WEST);
		panel.add(this.storyFile, BorderLayout.CENTER);

		final JButton browseButton = new JButton("Browse...");
		panel.add(browseButton, BorderLayout.EAST);
		browseButton.addActionListener((ev) -> {
			this.chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			this.chooser.setDialogTitle("Select the file containing the Gherkin Story");
			while (true) {
				switch (this.chooser.showDialog(null, "Select")) {
					case JFileChooser.APPROVE_OPTION:
						File target = this.chooser.getSelectedFile();
						try {
							target = target.getCanonicalFile();
						} catch (Exception e) {
							this.log.warn("Exception caught while trying to canonicalize the path [{}]", target, e);
						}
						this.storyFile.setText(target.getAbsolutePath());
						// fall-through

					default:
						return;
				}
			}
		});

		panel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED),
			"Gherkin Story file (overrides the Gherkin Story, below)", TitledBorder.LEFT, TitledBorder.TOP));
		return panel;
	}

	private JPanel createStoryPanel() {
		this.story = JSyntaxTextArea.getInstance(25, 80, false);
		final JScrollPane storyPane = JTextScrollPane.getInstance(this.story, true);
		this.story.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
		this.story.setLanguage("text");

		this.story.setWrapStyleWord(true);
		this.story.setTabSize(4);
		this.story.setLineWrap(false);

		final UndoManager undoManager = new UndoManager();
		KeyStroke undoKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_Z, Event.CTRL_MASK);
		KeyStroke redoKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_Y, Event.CTRL_MASK);

		Document document = this.story.getDocument();
		document.addUndoableEditListener(new UndoableEditListener() {
			@Override
			public void undoableEditHappened(UndoableEditEvent e) {
				undoManager.addEdit(e.getEdit());
			}
		});
		this.story.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(undoKeyStroke, "undoKeyStroke");
		this.story.getActionMap().put("undoKeyStroke", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					undoManager.undo();
				} catch (CannotUndoException cue) {
					// Ignore
				}
			}
		});
		this.story.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(redoKeyStroke, "redoKeyStroke");
		this.story.getActionMap().put("redoKeyStroke", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					undoManager.redo();
				} catch (CannotRedoException cre) {
					// Ignore
				}
			}
		});
		final JLabel label = new JLabel(
			"You can reference JMeter variables as ${varname} for substitution in the story (any valid JEXL3 syntax is supported)");
		label.setLabelFor(this.story);

		final JPanel panel = new JPanel(new BorderLayout());
		panel.add(label, BorderLayout.NORTH);
		panel.add(storyPane, BorderLayout.CENTER);
		panel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED), "Gherkin Story", TitledBorder.LEFT,
			TitledBorder.TOP));
		return panel;
	}

	@Override
	public String getStaticLabel() {
		return "Gherkin Story Sampler";
	}

	@Override
	public String getLabelResource() {
		return getClass().getCanonicalName();
	}

	@Override
	public void configure(TestElement element) {
		this.story.setText(element.getPropertyAsString(GherkinSampler.STORY));
		this.storyFile.setText(element.getPropertyAsString(GherkinSampler.STORY_FILE));
		this.parameters.setText(element.getPropertyAsString(GherkinSampler.PARAMETERS));
		super.configure(element);
	}

	@Override
	public TestElement createTestElement() {
		GherkinSampler sampler = new GherkinSampler();
		modifyTestElement(sampler);
		return sampler;
	}

	@Override
	public void modifyTestElement(TestElement element) {
		element.clear();
		configureTestElement(element);
		element.setProperty(GherkinSampler.STORY, this.story.getText());
		element.setProperty(GherkinSampler.STORY_FILE, this.storyFile.getText());
		element.setProperty(GherkinSampler.PARAMETERS, this.parameters.getText());
	}

	@Override
	public void clearGui() {
		super.clearGui();
		this.story.setText(GherkinSampler.DEFAULT_STORY);
	}
}
