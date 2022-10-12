package script.npc.olm;

public enum OlmPhase {
	
	ACID("The Great Olm rises with the power of <col=1aff1a>acid</col>."),
	
	FLAME("The Great Olm rises with the power of <col=ff1a1a>flame</col>."),
	
	CRYSTAL("The Great Olm rises with the power of <col=e600e6>crystal</col>."),
	
	OMEGA("");
	
	private String message;
	
	
	OlmPhase(String message) {
		this.message = message;
	}

	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

}
	