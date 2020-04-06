package fr.leomelki.loupgarou.scoreboard;

import java.util.Arrays;

import com.comphenix.protocol.wrappers.EnumWrappers.ScoreboardAction;

import fr.leomelki.com.comphenix.packetwrapper.WrapperPlayServerScoreboardScore;
import fr.leomelki.com.comphenix.packetwrapper.WrapperPlayServerScoreboardTeam;
import fr.leomelki.loupgarou.utils.VariousUtils;
import lombok.Getter;
import lombok.Setter;

public class CustomScoreboardEntry {
	
	//setter car flemme de modifier le systeme pour le rendre plus logique
	@Getter @Setter private int score;
	private final String name;
	private final CustomScoreboard scoreboard;
	private String prefix, suffix;

	public CustomScoreboardEntry(int score, CustomScoreboard scoreboard) {
		this.score = score;
		this.scoreboard = scoreboard;
		this.name = "§"+VariousUtils.toHex(score);
	}
	
	public void show() {
		if(prefix != null) {
			WrapperPlayServerScoreboardTeam team = new WrapperPlayServerScoreboardTeam();
			team.setPlayers(Arrays.asList(name));
			team.setName(name);
			team.setMode(0);
			team.setPrefix(prefix);
			if(suffix != null)
				team.setSuffix(suffix);
			team.sendPacket(scoreboard.getPlayer().getPlayer());
			
			WrapperPlayServerScoreboardScore score = new WrapperPlayServerScoreboardScore();
			score.setObjectiveName(scoreboard.getName());
			score.setScoreboardAction(ScoreboardAction.CHANGE);
			score.setScoreName(name);
			score.setValue(this.score);
			score.sendPacket(scoreboard.getPlayer().getPlayer());
		}
	}
	
	public void setDisplayName(String displayName) {
		boolean spawn = prefix == null;
		if(displayName.length() > 16) {
			char colorCode = 'f';
			int limit = displayName.charAt(14) == '§' && displayName.charAt(13) != '§' ? 14 : displayName.charAt(15) == '§' ? 15 : 16;
			prefix = displayName.substring(0, limit);
			
			if(limit == 16) {
				boolean storeColorCode = false;
				for(char c : prefix.toCharArray())
					if(storeColorCode) {
						storeColorCode = false;
						colorCode = c;
					}else
						if(c == '§')
							storeColorCode = true;
				suffix = "§"+colorCode+displayName.substring(limit);
			}else
				suffix = displayName.substring(limit);
		} else {
			prefix = displayName;
			suffix = "";
		}
		
		if(scoreboard.isShown()) {
			if(spawn)
				show();
			else {
				WrapperPlayServerScoreboardTeam team = new WrapperPlayServerScoreboardTeam();
				team.setPlayers(Arrays.asList(name));
				team.setName(name);
				team.setMode(2);
				team.setPrefix(prefix);
				if(suffix != null)
					team.setSuffix(suffix);
				team.sendPacket(scoreboard.getPlayer().getPlayer());
			}
		}
	}
	public void delete() {
		hide();
		prefix = null;
	}
	public void hide() {
		if(prefix != null && scoreboard.isShown()) {
			WrapperPlayServerScoreboardScore score = new WrapperPlayServerScoreboardScore();
			score.setObjectiveName(scoreboard.getName());
			score.setScoreboardAction(ScoreboardAction.REMOVE);
			score.setScoreName(name);
			score.sendPacket(scoreboard.getPlayer().getPlayer());
			
			WrapperPlayServerScoreboardTeam team = new WrapperPlayServerScoreboardTeam();
			team.setName(name);
			team.setMode(1);
			team.sendPacket(scoreboard.getPlayer().getPlayer());
		}
	}

}
