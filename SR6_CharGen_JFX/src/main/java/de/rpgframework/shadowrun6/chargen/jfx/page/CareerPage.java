package de.rpgframework.shadowrun6.chargen.jfx.page;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Locale;
import java.util.ResourceBundle;

import org.prelle.javafx.Mode;
import org.prelle.javafx.OptionalNodePane;
import org.prelle.javafx.Page;
import org.prelle.javafx.layout.FlexGridPane;

import de.rpgframework.ResourceI18N;
import de.rpgframework.genericrpg.data.ComplexDataItem;
import de.rpgframework.genericrpg.data.ComplexDataItemValue;
import de.rpgframework.jfx.GenericDescriptionVBox;
import de.rpgframework.shadowrun.Contact;
import de.rpgframework.shadowrun.ContactType;
import de.rpgframework.shadowrun.SIN;
import de.rpgframework.shadowrun.SIN.FakeRating;
import de.rpgframework.shadowrun6.Shadowrun6Tools;
import de.rpgframework.shadowrun6.chargen.charctrl.SR6CharacterController;
import de.rpgframework.shadowrun6.chargen.jfx.SR6CharacterViewLayout;
import de.rpgframework.shadowrun6.chargen.jfx.section.CreationSection;
import de.rpgframework.shadowrun6.chargen.jfx.section.SR6CareerHistorySection;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * @author prelle
 *
 */
public class CareerPage extends Page {

	private final static Logger logger = System.getLogger(CareerPage.class.getPackageName());

	private final static ResourceBundle RES = ResourceBundle.getBundle(SR6CharacterViewLayout.class.getName());

	private transient SR6CharacterController ctrl;

	private CreationSection secCreation;
	private SR6CareerHistorySection secHistory;
	private Button btnAddReward;
	private Button btnAddDowntime;
	private VBox bxHistory;
	private Runnable onChange;

	private FlexGridPane flex;
	private OptionalNodePane layout;

	//-------------------------------------------------------------------
	public CareerPage() {
		super(ResourceI18N.get(RES, "page.career.title"));
		initComponents();
		initLayout();
		initInteractivity();
	}

	//-------------------------------------------------------------------
	private void initComponents() {
		initCreationInfo();
		initHistoryInfo();
	}

	//-------------------------------------------------------------------
	private void initCreationInfo() {
		secCreation = new CreationSection();
		secCreation.setMaxHeight(Double.MAX_VALUE);
		FlexGridPane.setMinWidth(secCreation, 4);
		FlexGridPane.setMinHeight(secCreation, 6);
		FlexGridPane.setMediumWidth(secCreation, 5);
		FlexGridPane.setMediumHeight(secCreation, 6);
	}

	//-------------------------------------------------------------------
	/**
	 * Runs with their rewards and downtime actions in one history, with
	 * the buttons to add either of them above it
	 */
	private void initHistoryInfo() {
		btnAddReward = new Button(ResourceI18N.get(RES, "page.career.addReward"));
		btnAddDowntime = new Button(ResourceI18N.get(RES, "page.career.addDowntime"));
		secHistory = new SR6CareerHistorySection();
		secHistory.setTitle(ResourceI18N.get(RES, "page.career.history"));
		secHistory.setMaxHeight(Double.MAX_VALUE);
		VBox.setVgrow(secHistory, Priority.ALWAYS);

		bxHistory = new VBox(10, new HBox(10, btnAddReward, btnAddDowntime), secHistory);
		bxHistory.setMaxHeight(Double.MAX_VALUE);
		FlexGridPane.setMinWidth(bxHistory, 5);
		FlexGridPane.setMinHeight(bxHistory, 6);
		FlexGridPane.setMediumWidth(bxHistory, 5);
		FlexGridPane.setMediumHeight(bxHistory, 9);
	}

	//-------------------------------------------------------------------
	private void initLayout() {
		flex = new FlexGridPane();
		flex.setSpacing(20);
		flex.getChildren().addAll(secCreation, bxHistory);

		layout = new OptionalNodePane(flex, new Label(ResourceI18N.get(RES,"select.for.description")));
		setContent(layout);
		super.setMode(Mode.REGULAR);
	}

	//-------------------------------------------------------------------
	private void initInteractivity() {
//		secSINs.showHelpForProperty().addListener( (ov,o,n) -> showDescription(n));
		btnAddReward.setOnAction(ev -> secHistory.addReward());
		btnAddDowntime.setOnAction(ev -> secHistory.addDowntimeAction());
		secHistory.setOnChange(() -> {
			secCreation.refresh();
			if (onChange!=null) onChange.run();
		});
	}

	//-------------------------------------------------------------------
	private void showDescription(ComplexDataItemValue<? extends ComplexDataItem> n) {
		logger.log(Level.INFO, "Show description "+n);
		if (n==null) {
			layout.setOptional(null);
		} else {
			layout.setOptional( new GenericDescriptionVBox(Shadowrun6Tools.requirementResolver(Locale.getDefault()),
					Shadowrun6Tools.modificationResolver(Locale.getDefault()), n.getModifyable()));
			layout.setTitle(n.getModifyable().getName());
		}
	}

	//-------------------------------------------------------------------
	private void showDescription(Contact n) {
		logger.log(Level.INFO, "Show contact type "+n);
		if (n==null || n.getType()==null) {
			layout.setOptional(null);
		} else {
			ContactType t = n.getType();
			GenericDescriptionVBox desc = new GenericDescriptionVBox(Shadowrun6Tools.requirementResolver(Locale.getDefault()),
					Shadowrun6Tools.modificationResolver(Locale.getDefault()));
			desc.setData(t.getName(Locale.getDefault()), null, t.getDescription(Locale.getDefault()));
			layout.setOptional( desc);
			layout.setTitle(t.getName(Locale.getDefault()));
		}
	}

	//-------------------------------------------------------------------
	private void showDescription(SIN n) {
		logger.log(Level.INFO, "Show SIN "+n);
		if (n==null ) {
			layout.setOptional(null);
		} else {
			FakeRating rating = n.getQuality();
			GenericDescriptionVBox desc = new GenericDescriptionVBox( Shadowrun6Tools.requirementResolver(Locale.getDefault()),
					Shadowrun6Tools.modificationResolver(Locale.getDefault()));
			desc.setData(rating.name(), null, n.getDescription());
			layout.setOptional( desc);
			layout.setTitle(rating.name());
		}
	}

	//-------------------------------------------------------------------
	public void setController(SR6CharacterController ctrl) {
		logger.log(Level.INFO, "setController");
		if (ctrl==null)
			throw new NullPointerException("controller is null");
		this.ctrl = ctrl;

		secCreation.updateController(ctrl);
		secHistory.updateController(ctrl);

		refresh();
	}

	//-------------------------------------------------------------------
	/**
	 * @param onChange Called when a run or a downtime action changed the
	 *   character, so that other pages can show it
	 */
	public void setOnChange(Runnable onChange) {
		this.onChange = onChange;
	}

	//-------------------------------------------------------------------
	public void refresh() {
		secCreation.refresh();
		secHistory.refresh();
	}

}
