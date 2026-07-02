package xaos.tiles.entities.living.heroes;

import java.util.ArrayList;

import xaos.skills.SkillManager;
import xaos.utils.Messages;
import xaos.utils.Utils;

public class HeroSkills {

    private ArrayList<String> skills;
    private ArrayList<Integer> levels;

    public HeroSkills(String sSkills, String sLevels) throws Exception {
        this.skills = Utils.getArray(sSkills);
        ArrayList<String> alLevels = Utils.getArray(sLevels);

        if (this.skills == null || alLevels == null || this.skills.size() == 0) {
            throw new Exception(Messages.getString("HeroSkills.0")); //$NON-NLS-1$
        }

        this.levels = new ArrayList<Integer>();

        int iLevel;
        for (int i = 0; i < alLevels.size(); i++) {
            try {
                iLevel = Integer.parseInt(alLevels.get(i));
                this.levels.add(Integer.valueOf(iLevel));
            } catch (NumberFormatException nfe) {
                throw new Exception(Messages.getString("HeroSkills.1") + alLevels.get(i) + "]"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }

        if (this.skills.size() != this.levels.size()) {
            throw new Exception(Messages.getString("HeroSkills.3")); //$NON-NLS-1$
        }

        // Todo ok, miramos que las skills existan
        for (int i = 0; i < this.skills.size(); i++) {
            if (SkillManager.getItem(this.skills.get(i)) == null) {
                throw new Exception(Messages.getString("HeroSkills.2") + this.skills.get(i) + "]"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    }

    /**
     * Retorna una lista de skills id a partir de un level concreto
     *
     * @param level
     * @return
     */
    public ArrayList<String> getSkillsWhenReachLevel(int level) {
        ArrayList<String> alSkills = null;

        for (int i = 0; i < levels.size(); i++) {
            if (levels.get(i).intValue() == level) {
                if (alSkills == null) {
                    alSkills = new ArrayList<String>();
                }
                alSkills.add(skills.get(i));
            }
        }

        return alSkills;
    }
}
