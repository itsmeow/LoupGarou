package fr.leomelki.loupgarou.events;

import org.bukkit.event.Cancellable;

import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.localization.Translate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

public class LGPlayerKilledEvent extends LGEvent implements Cancellable{
	public LGPlayerKilledEvent(LGGame game, LGPlayer killed, Reason reason) {
		super(game);
		this.killed = killed;
		this.reason = reason;
	}

	@Getter @Setter boolean cancelled;
    
    @Getter @Setter private LGPlayer killed;
    @Getter @Setter private Reason reason;
	
    @RequiredArgsConstructor
	public static enum Reason{
		LOUP_GAROU,
		GM_LOUP_GAROU,
		LOUP_BLANC,
		SORCIERE,
		CHASSEUR_DE_VAMPIRE,
		VOTE,
		CHASSEUR,
		DICTATOR,
		DICTATOR_SUICIDE,
		DISCONNECTED,
		LOVE,
		BOUFFON,
		ASSASSIN,
		PYROMANE,
		PIRATE,
		FAUCHEUR,
		DONT_DIE;
		
	    public String getMessage(LGPlayer player, LGPlayer killed) {
	        return Translate.get(player, "killtype." + this.name().toLowerCase(), killed.getName());
	    }
	}
	
}
