package com.bartolini.pixelbyte.modules.terminal.commands;

import com.bartolini.pixelbyte.environment.Command;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class CmdLicense extends Command {

    public CmdLicense() {
        super("license", "Displays the license text (GNU GPL v.3).");
    }

    @Override
    public int execute(StringBuilder stringBuilder, List<String> args) {
        try {
            stringBuilder.append(Files.readString(Paths.get("LICENSE")));
        } catch (IOException | NullPointerException ex) {
            stringBuilder.append("[color=red]Could not load the license text.[/color]\n");
            stringBuilder.append("Please visit [i][color=blue]http://www.gnu.org/licenses/[/i][color] for a copy of the GNU GPL v.3 License.");
        }
        return 1;
    }
}