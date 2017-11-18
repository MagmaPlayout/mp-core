package playoutCore.melted.status;

import playoutCore.PlayoutCore;

/**
 * Process commands sent by mp-melted-status
 * 
 * @author rombus
 */
public class StatusCmdProcessor {
   private static final String MELTED_DISCONNECTED = "MELTED-DISCONNECTED";
   private final PlayoutCore core;
   
   public StatusCmdProcessor(PlayoutCore core){
       this.core = core;
   }
  
   public void processCmd(String cmd){
       System.out.println("PROCESS CMD "+cmd);
       if(cmd.equals(MELTED_DISCONNECTED)){
           core.reconnectToMelted();
       }
   }
}
