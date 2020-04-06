package fr.leomelki.loupgarou.classes;

import org.bukkit.Sound;

import lombok.Getter;

public enum LGSound {
	KILL(Sound.BLAZE_DEATH, "entity.blaze.death"),
	START_NIGHT(Sound.SKELETON_DEATH, "entity.skeleton.death"),
	START_DAY(Sound.ZOMBIE_DEATH, "entity.zombie.death");
	
	@Getter Sound sound;
	@Getter String id;
	LGSound(Sound sound, String id){
		this.sound = sound;
		this.id = id;
	}
}
