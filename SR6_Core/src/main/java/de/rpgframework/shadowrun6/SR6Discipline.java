package de.rpgframework.shadowrun6;

import java.util.Locale;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import de.rpgframework.genericrpg.data.AttributeValue;
import de.rpgframework.shadowrun.IDiscipline;
import de.rpgframework.shadowrun.MagicOrResonanceType;
import de.rpgframework.shadowrun.ShadowrunAttribute;
import de.rpgframework.shadowrun.ShadowrunCharacter;
import de.rpgframework.shadowrun.Tradition;
import de.rpgframework.shadowrun6.proc.CalculateEssence;

/**
 * The disciplines of SR7 (core rules, chapter 5 "Disciplines" and chapter 6
 * "Skill Mastery")
 */
public enum SR6Discipline implements IDiscipline {

	//            Mastery Skill group       Requirement                                   Maximum rating
	INITIATION    (false, null,     m -> usesMagic(m),                            m -> traditionAttribute(m)),
	SUBMERSION    (false, null,     m -> usesResonance(m),                        m -> attribute(m, ShadowrunAttribute.RESONANCE)),
	TRANSHUMANISM (false, null,     m -> isMundane(m) && augmentationLevel(m)>=1, m -> augmentationLevel(m)),
	DRACOGENESIS_POWER(true,  null, m -> false,        x -> SR6Discipline.MAX_RATING),
	ANIMALISM     (false, null, m -> false, x -> SR6Discipline.MAX_RATING)
	;

	/** No discipline can be rated higher */
	public final static int MAX_RATING = 12;

	private final boolean mastery;
	private final String skillGroup;
	private final Predicate<Shadowrun6Character> requirement;
	private final ToIntFunction<Shadowrun6Character> maxGrade;

	//-------------------------------------------------------------------
	SR6Discipline(boolean mastery, String skillGroup, Predicate<Shadowrun6Character> req, ToIntFunction<Shadowrun6Character> maxGrade) {
		this.mastery    = mastery;
		this.skillGroup = skillGroup;
		this.requirement = req;
		this.maxGrade   = maxGrade;
	}

	//-------------------------------------------------------------------
	private static boolean usesMagic(Shadowrun6Character model) {
		return model.getMagicOrResonanceType()!=null && model.getMagicOrResonanceType().usesMagic();
	}

	//-------------------------------------------------------------------
	private static boolean usesResonance(Shadowrun6Character model) {
		return model.getMagicOrResonanceType()!=null && model.getMagicOrResonanceType().usesResonance();
	}

	//-------------------------------------------------------------------
	private static boolean isMundane(Shadowrun6Character model) {
		return !usesMagic(model) && !usesResonance(model);
	}

	//-------------------------------------------------------------------
	private static int augmentationLevel(Shadowrun6Character model) {
		return -1; // CalculateEssence.getAugmentationLevel(model);
	}

	//-------------------------------------------------------------------
	private static int attribute(Shadowrun6Character model, ShadowrunAttribute attr) {
		AttributeValue<ShadowrunAttribute> val = model.getAttribute(attr);
		return (val!=null) ? val.getModifiedValue() : 0;
	}

	//-------------------------------------------------------------------
	/**
	 * The initiate rating cannot exceed the tradition attribute. Mystic adepts
	 * without a tradition use Body (chapter 8 "Mystic Adepts"), adepts only the
	 * general maximum.
	 */
	private static int traditionAttribute(Shadowrun6Character model) {
		Tradition trad = model.getTradition();
		if (trad!=null && trad.getTraditionAttribute()!=null)
			return attribute(model, trad.getTraditionAttribute());
		MagicOrResonanceType mor = model.getMagicOrResonanceType();
		if (mor!=null && mor.usesPowers() && mor.usesSpells())
			return attribute(model, ShadowrunAttribute.BODY);
		return MAX_RATING;
	}

	//-------------------------------------------------------------------
	public String getName() {
		return getName(Locale.getDefault());
	}

	//-------------------------------------------------------------------
	public String getName(Locale loc) {
		return Shadowrun6Core.getI18nResources().getString("discipline."+this.name().toLowerCase(), loc);
	}

	//-------------------------------------------------------------------
	/**
	 * @return TRUE for skill masteries, which may be combined with each other
	 *   and every other discipline
	 */
	public boolean isMastery() {
		return mastery;
	}

	//-------------------------------------------------------------------
	/**
	 * @return The ID of the skill group of a skill mastery, NULL otherwise
	 */
	public String getSkillGroup() {
		return skillGroup;
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.shadowrun.IDiscipline#hasOwnGrade()
	 */
	@Override
	public boolean hasOwnGrade() {
		return mastery;
	}

	//-------------------------------------------------------------------
	@SuppressWarnings("rawtypes")
	public boolean canBeSelected(ShadowrunCharacter model) {
		return requirement.test((Shadowrun6Character) model);
	}

	//-------------------------------------------------------------------
	/**
	 * @return The highest rating the character may reach in this discipline
	 */
	public int getMaxGrade(Shadowrun6Character model) {
		return Math.min(MAX_RATING, maxGrade.applyAsInt(model));
	}

}
