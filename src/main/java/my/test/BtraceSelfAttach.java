package my.test;

import net.bytebuddy.agent.ByteBuddyAgent;

public class BtraceSelfAttach {
    public static void main(String[] args) {
        //BTraceEngine.newInstance().createTask(SelfPid.getSelfPidPre9());
        ByteBuddyAgent.install();
    }
}
