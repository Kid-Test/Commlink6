package de.rpgframework.shadowrun6.chargen.jfx.pane;

import java.util.Arrays;
import java.util.List;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import org.prelle.javafx.TitledComponent;

import de.rpgframework.ResourceI18N;
import de.rpgframework.genericrpg.Reward;
import de.rpgframework.genericrpg.modification.ValueModification;
import de.rpgframework.jfx.pane.RewardPane;
import de.rpgframework.shadowrun.Contact;
import de.rpgframework.shadowrun.ShadowrunAttribute;
import de.rpgframework.shadowrun6.modifications.ShadowrunReference;
import javafx.geometry.Orientation;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;

/**
 * @author prelle
 *
 */
public class SR6RewardPane extends RewardPane {

	private static PropertyResourceBundle RES = (PropertyResourceBundle) ResourceBundle.getBundle(SR6RewardPane.class.getPackageName()+".Panes");
	
	private TextField tfKarma;
	private TextField tfNuyen;
	private TextField tfHeat;
	private TextField tfRep;
	private TextField tfContacts;
	private TextArea taNote;

	//-------------------------------------------------------------------
	public SR6RewardPane(Reward data) {
		super();
		setReward(data);
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.jfx.pane.RewardPane#initComponents()
	 */
	protected void initComponents() {
		super.initComponents();
		tfKarma = new TextField(); tfKarma.setPrefColumnCount(2);
		tfNuyen = new TextField(); tfNuyen.setPrefColumnCount(6);
		tfHeat  = new TextField(); tfHeat .setPrefColumnCount(2);
		tfRep   = new TextField(); tfRep  .setPrefColumnCount(2);
		tfContacts = new TextField();
		tfContacts.setPrefColumnCount(30);
		tfContacts.setPromptText(ResourceI18N.get(RES, "pane.reward.contacts.prompt"));
		taNote = new TextArea();
		taNote.setPrefRowCount(3);
		taNote.setWrapText(true);
	}
	
	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.jfx.pane.RewardPane#initLayout()
	 */
	protected void initLayout() {
		super.initLayout();
		
		getChildren().add(new Separator(Orientation.HORIZONTAL));
		TitledComponent tcKarma = new TitledComponent(ResourceI18N.get(RES, "pane.reward.karma"), tfKarma).setTitleMinWidth(60d);
		TitledComponent tcNuyen = new TitledComponent(ResourceI18N.get(RES, "pane.reward.nuyen"), tfNuyen).setTitleMinWidth(60d);
		TitledComponent tcHeat  = new TitledComponent(ResourceI18N.get(RES, "pane.reward.heat"), tfHeat).setTitleMinWidth(60d);
		TitledComponent tcRep   = new TitledComponent(ResourceI18N.get(RES, "pane.reward.reputation"), tfRep).setTitleMinWidth(60d);

		FlowPane flow = new FlowPane(tcKarma, tcNuyen, tcHeat, tcRep);
		flow.setVgap(5);
		flow.setHgap(10);
		getChildren().add(flow);

		TitledComponent tcContacts = new TitledComponent(ResourceI18N.get(RES, "pane.reward.contacts"), tfContacts).setTitleMinWidth(110d);
		TitledComponent tcNote     = new TitledComponent(ResourceI18N.get(RES, "pane.reward.note"), taNote).setTitleMinWidth(110d);
		getChildren().addAll(tcContacts, tcNote);
	}

	//-------------------------------------------------------------------
	protected void initInteractivity() {
		super.initInteractivity();

		taNote.textProperty().addListener( (ov,o,n) -> reward.setNote( (n==null || n.isBlank()) ? null : n.trim()));

		tfKarma.textProperty().addListener( (ov,o,n) -> {
			if (n==null || n.isBlank()) { super.reward.setExperiencePoints(0); return; }
			try {
				reward.setExperiencePoints( Integer.parseInt(n));
			} catch (NumberFormatException e) {
				reward.setExperiencePoints(0); 
			}
		});
		
		tfNuyen.textProperty().addListener( (ov,o,n) -> {
			if (n==null || n.isBlank()) { super.reward.setMoney(0); return; }
			try {
				reward.setMoney( Integer.parseInt(n));
			} catch (NumberFormatException e) {
				reward.setMoney(0); 
			}
		});
		
		tfHeat.textProperty().addListener( (ov,o,n) -> {
			reward.removeModification(ShadowrunReference.ATTRIBUTE, ShadowrunAttribute.HEAT.name());
			if (n==null || n.isBlank()) { return; }
			try {
				reward.addIncomingModification( new ValueModification(ShadowrunReference.ATTRIBUTE, ShadowrunAttribute.HEAT.name(), Integer.parseInt(n)) );
			} catch (NumberFormatException e) {
			}
		});
		
		tfRep.textProperty().addListener( (ov,o,n) -> {
			reward.removeModification(ShadowrunReference.ATTRIBUTE, ShadowrunAttribute.REPUTATION.name());
			if (n==null || n.isBlank()) { return; }
			try {
				reward.addIncomingModification( new ValueModification(ShadowrunReference.ATTRIBUTE, ShadowrunAttribute.REPUTATION.name(), Integer.parseInt(n)) );
			} catch (NumberFormatException e) {
			}
		});
	}

	//-------------------------------------------------------------------
	private static Contact contactNamed(String name) {
		Contact ret = new Contact();
		ret.setName(name);
		return ret;
	}

	//-------------------------------------------------------------------
	/**
	 * @return Contacts met during the run - one per name, to be detailed
	 *   on the contacts page
	 */
	public List<Contact> getNewContacts() {
		return Arrays.stream(tfContacts.getText().split(","))
				.map(String::trim)
				.filter(name -> !name.isEmpty())
				.map(SR6RewardPane::contactNamed)
				.toList();
	}

}
