package de.rpgframework.shadowrun6.chargen.jfx.section;

import java.util.List;

import de.rpgframework.genericrpg.Reward;
import de.rpgframework.genericrpg.modification.ModifiedObjectType;
import de.rpgframework.jfx.pane.RewardPane;
import de.rpgframework.shadowrun.chargen.charctrl.IDowntimeController;
import de.rpgframework.shadowrun.chargen.jfx.section.CareerHistorySection;
import de.rpgframework.shadowrun6.Shadowrun6Tools;
import de.rpgframework.shadowrun6.chargen.charctrl.SR6CharacterController;
import de.rpgframework.shadowrun6.chargen.jfx.pane.SR6RewardPane;
import de.rpgframework.shadowrun6.modifications.ShadowrunReference;

/**
 * Runs and downtime actions of a Shadowrun 6 character
 *
 * @author prelle
 */
public class SR6CareerHistorySection extends CareerHistorySection {

	private SR6CharacterController control;

	// ---
	public SR6CareerHistorySection() {
	}

	// ---
	@Override
	protected boolean hasCharacter() { return control!=null; }

	// ---
	@Override
	protected List<Reward> getRewards() { return control.getModel().getRewards(); }

	// ---
	@Override
	protected IDowntimeController getDowntimeController() { return control.getDowntimeController(); }

	// ---
	@Override
	protected ModifiedObjectType attributeType() { return ShadowrunReference.ATTRIBUTE; }

	// ---
	@Override
	protected RewardPane createRewardPane() { return new SR6RewardPane(null); }

	// ---
	@Override
	protected void applyReward(RewardPane pane) {
		Shadowrun6Tools.applyReward(control.getModel(), pane.getData(), ((SR6RewardPane)pane).getNewContacts());
	}

	// ---
	@Override
	protected void removeReward(Reward reward) { Shadowrun6Tools.removeReward(control.getModel(), reward); }

	// ---
	@Override
	protected int countContacts(Reward reward) { return Shadowrun6Tools.getContactsOfReward(reward).size(); }

	// ---
	@Override
	protected void runProcessors() { control.runProcessors(); }

	// ---
	public void updateController(SR6CharacterController ctrl) {
		this.control = ctrl;
		refresh();
	}

}
