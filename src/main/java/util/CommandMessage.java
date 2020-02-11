package util;

public class CommandMessage extends Message {
    private String command;
    private String commandArgument;

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getCommandArgument() {
        return commandArgument;
    }

    public void setCommandArgument(String commandArgument) {
        this.commandArgument = commandArgument;
    }
}
