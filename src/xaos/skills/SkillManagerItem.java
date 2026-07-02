package xaos.skills;

import java.util.ArrayList;

import xaos.utils.Messages;
import xaos.utils.Utils;

public class SkillManagerItem {

    public final static int USE_UNKNOWN = 0;
    public final static int USE_ALWAYS = 1;
    public final static int USE_HITTED = 2;
    public final static int USE_ENEMIES_IN_LOS = 3;
    public final static int USE_NEAR_DEATH = 4;
    public final static int USE_NOT_MAX_HP = 5;

    public final static String USE_STR_ALWAYS = "ALWAYS"; //$NON-NLS-1$
    public final static String USE_STR_HITTED = "HITTED"; //$NON-NLS-1$
    public final static String USE_STR_ENEMIES_IN_LOS = "ENEMIES_IN_LOS"; //$NON-NLS-1$
    public final static String USE_STR_NEAR_DEATH = "NEAR_DEATH"; //$NON-NLS-1$
    public final static String USE_STR_NOT_MAX_HP = "NOT_MAX_HP"; //$NON-NLS-1$

    private String iniHeader;
    private String name;
    private String coolDown;
    private int use;

    // Especiales
    private boolean taunt;
    private String raiseDead;
    private int maxRaised;

    private ArrayList<SkillEffectItem> effects;

    public void setIniHeader(String iniHeader) {
        this.iniHeader = iniHeader;
    }

    public String getIniHeader() {
        return iniHeader;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setCoolDown(String coolDown) {
        this.coolDown = coolDown;
    }

    public String getCoolDown() {
        return coolDown;
    }

    public void setUse(int use) {
        this.use = use;
    }

    public void setUse(String sUse) throws Exception {
        if (sUse == null || sUse.length() == 0) {
            throw new Exception(Messages.getString("SkillManagerItem.3")); //$NON-NLS-1$
        }

        if (sUse.equalsIgnoreCase(USE_STR_ALWAYS)) {
            setUse(USE_ALWAYS);
        } else if (sUse.equalsIgnoreCase(USE_STR_HITTED)) {
            setUse(USE_HITTED);
        } else if (sUse.equalsIgnoreCase(USE_STR_ENEMIES_IN_LOS)) {
            setUse(USE_ENEMIES_IN_LOS);
        } else if (sUse.equalsIgnoreCase(USE_STR_NEAR_DEATH)) {
            setUse(USE_NEAR_DEATH);
        } else if (sUse.equalsIgnoreCase(USE_STR_NOT_MAX_HP)) {
            setUse(USE_NOT_MAX_HP);
        } else {
            throw new Exception(Messages.getString("SkillManagerItem.4") + sUse + "]"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    public int getUse() {
        return use;
    }

    public void setTaunt(boolean taunt) {
        this.taunt = taunt;
    }

    public void setTaunt(String sTaunt) {
        setTaunt(Boolean.parseBoolean(sTaunt));
    }

    public boolean isTaunt() {
        return taunt;
    }

    public void setRaiseDead(String raiseDead) {
        this.raiseDead = raiseDead;
    }

    public String getRaiseDead() {
        return raiseDead;
    }

    public void setMaxRaised(int maxRaised) {
        this.maxRaised = maxRaised;
    }

    public void setMaxRaised(String sMaxRaised) {
        setMaxRaised(Utils.getInteger(sMaxRaised, 0));
    }

    public int getMaxRaised() {
        return maxRaised;
    }

    public void setEffects(ArrayList<SkillEffectItem> effects) {
        this.effects = effects;
    }

    public ArrayList<SkillEffectItem> getEffects() {
        return effects;
    }
}
