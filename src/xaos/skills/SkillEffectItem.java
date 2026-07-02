package xaos.skills;

import xaos.utils.Messages;

public class SkillEffectItem {

    public static final String TARGET_SELF = "SELF"; //$NON-NLS-1$
    public static final String TARGET_FRIENDLIES = "FRIENDLIES"; //$NON-NLS-1$
    public static final String TARGET_ENEMIES = "ENEMIES"; //$NON-NLS-1$

    public static final int TARGET_INT_NONE = 0;
    public static final int TARGET_INT_SELF = 1;
    public static final int TARGET_INT_FRIENDLIES = 2;
    public static final int TARGET_INT_ENEMIES = 3;

    private String id;
    private int target;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getTarget() {
        return target;
    }

    public void setTarget(int target) {
        this.target = target;
    }

    public void setTarget(String sTarget) throws Exception {
        if (sTarget == null || sTarget.length() == 0) {
            setTarget(TARGET_INT_NONE);
        } else {
            if (sTarget.equalsIgnoreCase(TARGET_SELF)) {
                setTarget(TARGET_INT_SELF);
            } else if (sTarget.equalsIgnoreCase(TARGET_FRIENDLIES)) {
                setTarget(TARGET_INT_FRIENDLIES);
            } else if (sTarget.equalsIgnoreCase(TARGET_ENEMIES)) {
                setTarget(TARGET_INT_ENEMIES);
            } else {
                throw new Exception(Messages.getString("EffectData.2") + sTarget + "]"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    }
}
