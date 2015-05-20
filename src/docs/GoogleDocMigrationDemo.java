/* Copyright (c) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package docs;

import info.bliki.html.HTML2WikiConverter;
import info.bliki.html.wikipedia.ToWikipedia;
import net.sourceforge.jwbf.core.contentRep.Article;
import net.sourceforge.jwbf.mediawiki.bots.MediaWikiBot;
import sample.util.SimpleCommandLineParser;
import com.google.gdata.data.Link;
import com.google.gdata.data.acl.AclEntry;
import com.google.gdata.data.docs.DocumentListEntry;
import com.google.gdata.data.docs.DocumentListFeed;
import com.google.gdata.data.docs.RevisionEntry;
import com.google.gdata.data.docs.RevisionFeed;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * An application that serves as a sample to show how the Documents List
 * Service can be used to search your documents and upload files.
 *
 *
 *
 *
 */
public class GoogleDocMigrationDemo {
    private DocumentList documentList;
    private PrintStream out;
    private MediaWikiBot bot;

    private static final String APPLICATION_NAME = "JavaGDataClientSampleAppV3.0";

    /**
     * The message for displaying the usage parameters.
     */
    private static final String[] USAGE_MESSAGE = {
            "Usage: java GoogleDocMigrationDemo.jar --username <user> --password <pass>",
            "Usage: java GoogleDocMigrationDemo.jar --authSub <token>",
            "    [--host <host:port>]          Where is the feed (default = docs.google.com)",
            "    [--log]                       Enable logging of requests",
            ""};

    /**
     * Welcome message, introducing the program.
     */
    private final String[] WELCOME_MESSAGE = {
            "", "This is a demo of the GoogleDoc migration!",
            "Using this interface, you can list and migrate your Google Docs.",
            "Type 'help' for a list of commands.", ""};

    /**
     * Help on all available commands.
     */
    private final String[] COMMAND_HELP_MESSAGE = {
            "Commands:",
            "    list [object_type] [...]                  [[lists objects]]",
            "    migrate <resource_id> <folder_id>            [[migrate a document to Wiki]]",
            "    revisions <resource_id>                   [[lists revisions of a document]]",
            "",
            "    help [command]                            [[display this message, or info about"
                    + " the specified command]]",
            "    exit                                      [[exit the program]]"};

    private final String[] COMMAND_HELP_LIST = {
            "list [object_type]",
            "    object_type: all, starred, documents, spreadsheets, pdfs, presentations, folders.\n"
                    + "        (defaults to 'all')", "list folder <folder_id>",
            "    folder_id: The id of the folder you want the contents list for."};

    private final String[] COMMAND_HELP_SEARCH = {
            "search <search_text>",
            "    search_text: A string to be used for a full text query"};
    private final String[] COMMAND_HELP_ASEARCH = {
            "asearch [<query_param>=<value>] [<query_param2>=<value2>] ...",
            "    query_param: title, title-exact, opened-min, opened-max, owner, writer, reader, "
                    + "showfolders, etc.", "    value: The value of the parameter"};

    private final String[] COMMAND_HELP_REVISIONS = {
            "revisions <resource_id>", "    resource_id: document resource id"};
    private final String[] COMMAND_HELP_HELP = {
            "help [command]", "    Weeeeeeeeeeeeee..."};
    private final String[] COMMAND_HELP_EXIT = {
            "exit", "    Exit the program."};
    private final String[] COMMAND_HELP_ERROR = {"unknown command"};
    private final String[] COMMAND_MIGRATE_HELP = {
            "migrate resource_id [category]", "Migrate the resource with resource ID under the category"
    };

    private static final String CHT_ROOT = "CloudHealth";
    private static final String TMP_FILE = "/tmp/tmp.html";
    private static final String DEFAULT_CATEGORY = "Default";

    private final Map<String, String[]> HELP_MESSAGES;
    {
        HELP_MESSAGES = new HashMap<String, String[]>();
        HELP_MESSAGES.put("list", COMMAND_HELP_LIST);
        HELP_MESSAGES.put("search", COMMAND_HELP_SEARCH);
        HELP_MESSAGES.put("asearch", COMMAND_HELP_ASEARCH);
        HELP_MESSAGES.put("revisions", COMMAND_HELP_REVISIONS);
        HELP_MESSAGES.put("help", COMMAND_HELP_HELP);
        HELP_MESSAGES.put("exit", COMMAND_HELP_EXIT);
        HELP_MESSAGES.put("error", COMMAND_HELP_ERROR);
        HELP_MESSAGES.put("migrate", COMMAND_MIGRATE_HELP);
    }

    /**
     * Constructor
     *
     * @param outputStream Stream to print output to.
     * @throws DocumentListException
     */
    public GoogleDocMigrationDemo(PrintStream outputStream, String appName, String host)
            throws DocumentListException {
        out = outputStream;
        documentList = new DocumentList(appName, host);
        bot = new MediaWikiBot("http://localhost/wiki/index.php");
        bot.login("your_user_name", "your_password");
    }

    /**
     * Authenticates the client using ClientLogin
     *
     * @param username User's email address
     * @param password User's password
     * @throws DocumentListException
     * @throws AuthenticationException
     */
    public void login(String username, String password) throws AuthenticationException,
            DocumentListException {
        documentList.login(username, password);
    }

    /**
     * Authenticates the client using AuthSub
     *
     * @param authSubToken authsub authorization token.
     * @throws DocumentListException
     * @throws AuthenticationException
     */
    public void login(String authSubToken)
            throws AuthenticationException, DocumentListException {
        documentList.loginWithAuthSubToken(authSubToken);
    }

    /**
     * Prints out the specified document entry.
     *
     * @param doc the document entry to print.
     */
    public void printDocumentEntry(DocumentListEntry doc) {
        StringBuffer output = new StringBuffer();

        output.append(" -- " + doc.getTitle().getPlainText() + " ");
        if (!doc.getParentLinks().isEmpty()) {
            for (Link link : doc.getParentLinks()) {
                output.append("[" + link.getTitle() + "] ");
            }
        }
        output.append(doc.getResourceId());

        out.println(output);
    }

    /**
     * Prints out the specified revision entry.
     */
    public void printRevisionEntry(RevisionEntry entry) {
        StringBuffer output = new StringBuffer();

        output.append(" -- " + entry.getTitle().getPlainText());
        output.append(", created on " + entry.getUpdated().toUiString() + " ");
        output.append(" by " + entry.getModifyingUser().getName() + " - "
                + entry.getModifyingUser().getEmail() + "\n");
        output.append("    " + entry.getHtmlLink().getHref());

        out.println(output);
    }

    /**
     * Prints out the specified ACL entry.
     *
     * @param entry the ACL entry to print.
     */
    public void printAclEntry(AclEntry entry) {
        out.println(" -- " + entry.getScope().getValue() + ": " + entry.getRole().getValue());
    }



    static String readFile(String path, Charset encoding)
            throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }


    /**
     * Execute the "list" command.
     *
     * @param args arguments for the "list" command.
     *     args[0] = "list"
     *     args[1] = category ("all", "folders", "documents", "spreadsheets", "pdfs",
     *        "presentations", "starred", "trashed")
     *     args[2] = folderId (required if args[1] is "folder")
     *
     * @throws IOException when an error occurs in communication with the Doclist
     *         service.
     * @throws MalformedURLException when an malformed URL is used.
     * @throws ServiceException when the request causes an error in the Doclist
     *         service.
     * @throws DocumentListException
     */
    private void executeList(String[] args) throws IOException,
            ServiceException, DocumentListException {
        DocumentListFeed feed = null;
        String msg = "";

        switch (args.length) {
            case 1:
                msg = "List of docs: ";
                feed = documentList.getDocsListFeed("all");
                break;
            case 2:
                msg = "List of all " + args[1] + ": ";
                feed = documentList.getDocsListFeed(args[1]);
                break;
            case 3:
                if (args[1].equals("folder")) {
                    msg = "Contents of folder_id '" + args[2] + "': ";
                    feed = documentList.getFolderDocsListFeed(args[2]);
                }
                break;
        }

        if (feed != null) {
            out.println(msg);
            for (DocumentListEntry entry : feed.getEntries()) {
                printDocumentEntry(entry);
            }
        } else {
            printMessage(COMMAND_HELP_LIST);
        }
    }


    /**
     * Execute the "search" command.
     *
     * @param args arguments for the "search" command.
     *     args[0] = "search"
     *     args[1] = searchString
     *
     * @throws IOException when an error occurs in communication with the Doclist
     *         service.
     * @throws MalformedURLException when an malformed URL is used.
     * @throws ServiceException when the request causes an error in the Doclist
     *         service.
     * @throws DocumentListException
     */
    private void executeSearch(String[] args) throws IOException,
            ServiceException, DocumentListException {
        if (args.length == 2) {
            HashMap<String, String> searchParameters = new HashMap<String, String>();
            searchParameters.put("q", args[1]);

            DocumentListFeed feed = documentList.search(searchParameters);
            out.println("Results for [" + args[1] + "]");
            for (DocumentListEntry entry : feed.getEntries()) {
                printDocumentEntry(entry);
            }
        } else {
            printMessage(COMMAND_HELP_SEARCH);
        }
    }

    /**
     * Execute the "asearch" (advanced search) command.
     *
     * @param args arguments for the "asearch" command.
     *     args[0] = "asearch"
     *
     * @throws IOException when an error occurs in communication with the Doclist
     *         service.
     * @throws MalformedURLException when an malformed URL is used.
     * @throws ServiceException when the request causes an error in the Doclist
     *         service.
     * @throws DocumentListException
     */
    private void executeAdvancedSearch(String[] args) throws IOException,
            ServiceException, DocumentListException {
        if (args.length <= 1) {
            printMessage(COMMAND_HELP_ASEARCH);
            return;
        }

        HashMap<String, String> searchParameters = new HashMap<String, String>();
        for (int i = 1; i < args.length; ++i) {
            searchParameters.put(args[i].substring(0, args[i].indexOf("=")), args[i]
                    .substring(args[i].indexOf("=") + 1));
        }

        DocumentListFeed feed = documentList.search(searchParameters);
        out.println("Results for advanced search:");
        for (DocumentListEntry entry : feed.getEntries()) {
            printDocumentEntry(entry);
        }
    }

    /**
     * Execute the "revisions" command.
     *
     * @param args arguments for the "upload" command.
     *     args[0] = "revisions"
     *     args[1] = resourceId (the resource id of the object to fetch revisions for)
     *
     * @throws IOException when an error occurs in communication with the Doclist
     *         service.
     * @throws MalformedURLException when an malformed URL is used.
     * @throws ServiceException when the request causes an error in the Doclist
     *         service.
     * @throws DocumentListException
     */
    private void executeRevisions(String[] args) throws IOException,
            ServiceException, DocumentListException {
        if (args.length == 2) {
            RevisionFeed feed = documentList.getRevisionsFeed(args[1]);
            if (feed != null) {
                out.println("List of revisions...");
                for (RevisionEntry entry : feed.getEntries()) {
                    printRevisionEntry(entry);
                }
            } else {
                printMessage(COMMAND_HELP_REVISIONS);
            }
        } else {
            printMessage(COMMAND_HELP_REVISIONS);
        }
    }

    /**
     * Execute the "help" command.
     *
     * @param args arguments for the "help" command.
     *     args[0] = "help"
     *     args[1] = command
     */
    private void executeHelp(String[] args) {
        if (args.length == 1) {
            printMessage(COMMAND_HELP_MESSAGE);
        } else if (args.length == 2) {
            if (HELP_MESSAGES.containsKey(args[1])) {
                printMessage(HELP_MESSAGES.get(args[1]));
            } else {
                printMessage(HELP_MESSAGES.get("error"));
            }
        } else {
            printMessage(COMMAND_HELP_MESSAGE);
        }
    }

    /**
     * Gets the type of file from the extension on the filename.
     *
     * @param filename the filename to extract the type of file from.
     */
    private String getTypeFromFilename(String filename) {
        return filename.substring(filename.lastIndexOf(".") + 1);
    }


    /**
     * Parses the command entered by the user into individual arguments.
     *
     * @param command the entire command entered by the user to be broken up into
     *        arguments.
     */
    private String[] parseCommand(String command) {
        // Special cases:
        if (command.startsWith("search")) {
            // if search command, only break into two args (command, search_string)
            return command.trim().split(" ", 2);
        } else if (command.startsWith("create")) {
            // if create command, break into three args (command, file_type, title)
            return command.trim().split(" ", 3);
        } else if (command.startsWith("upload")) {
            // if upload command, break into three args (command, file_path, title)
            return command.trim().split(" ", 3);
        }

        // Default case, split into n args using a space as the separator.
        return command.trim().split(" ");

    }

    /**
     * Reads and executes one command.
     *
     * @param reader to read input from the keyboard
     * @return false if the user quits, true on exception
     * @throws IOException
     * @throws ServiceException
     */
    private boolean executeCommand(BufferedReader reader)
            throws IOException, ServiceException, InterruptedException {
        System.err.print("Command: ");

        try {
            String command = reader.readLine();
            if (command == null) {
                return false;
            }

            String[] args = parseCommand(command);
            String name = args[0];

            if (name.equals("search")) {
                executeSearch(args);
            } else if (name.equals("asearch")) {
                executeAdvancedSearch(args);
            } else if (name.equals("revisions")) {
                executeRevisions(args);
            } else if (name.equals("help")) {
                executeHelp(args);
            } else if (name.equals("list")) {
                executeList(args);
            } else if (name.equals("migrate")) {
                executeMigration(args);
            } else if (name.startsWith("q") || name.startsWith("exit")) {
                return false;
            } else {
                out.println("Unknown command. Type 'help' for a list of commands.");
            }
        } catch (DocumentListException e) {
            // Show *exactly* what went wrong.
            e.printStackTrace();
        }
        return true;
    }

    private void executeMigration(String[] args) {
        try {
            if (args.length == 3 || args.length == 2) {
                documentList.downloadDocument(args[1], TMP_FILE, "html");
                HTML2WikiConverter conv = new HTML2WikiConverter();
                conv.setInputHTML(readFile(TMP_FILE, StandardCharsets.UTF_8));
                String content = conv.toWiki(new ToWikipedia());
                DocumentListEntry entry = documentList.getDocsListEntry(args[1]);
                String title = entry.getTitle().getPlainText();
                String category = DEFAULT_CATEGORY;
                if (entry.getParentLinks() != null && entry.getParentLinks().size() > 0) {
                    category = entry.getParentLinks().get(0).getTitle();
                }
                if (args.length == 3) {
                    category = args[2];
                }
                Article root = bot.getArticle(CHT_ROOT);
                if (!root.getText().contains("[[" + category + "]]")) {
                    root.addText("\n*[[" + category + "]]");
                    root.save();
                }
                Article a = bot.getArticle(category);
                a.addText("\n*[[" + title + "]]");
                a.save();
                a = bot.getArticle(title);
                a.addText(content);
                a.save();
                System.out.println("The document \"" + title + "\" is successfully migrated under \"" + category + "\"");
            } else {
                printMessage(COMMAND_MIGRATE_HELP);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Starts up the demo and prompts for commands.
     *
     * @throws ServiceException
     * @throws IOException
     */
    public void run() throws IOException, ServiceException, InterruptedException {
        printMessage(WELCOME_MESSAGE);

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        while (executeCommand(reader)) {
        }
    }

    /**
     * Prints out a message.
     *
     * @param msg the message to be printed.
     */
    private static void printMessage(String[] msg) {
        for (String s : msg) {
            System.out.println(s);
        }
    }

    private static void turnOnLogging() {
        // Configure the logging mechanisms
        Logger httpLogger =
                Logger.getLogger("com.google.gdata.client.http.HttpGDataRequest");
        httpLogger.setLevel(Level.ALL);
        Logger xmlLogger = Logger.getLogger("com.google.gdata.util.XmlParser");
        xmlLogger.setLevel(Level.ALL);

        // Create a log handler which prints all log events to the console
        ConsoleHandler logHandler = new ConsoleHandler();
        logHandler.setLevel(Level.ALL);
        httpLogger.addHandler(logHandler);
        xmlLogger.addHandler(logHandler);
    }

    /**
     * Runs the demo.
     *
     * @param args the command-line arguments
     *
     * @throws DocumentListException
     * @throws ServiceException
     * @throws IOException
     */
    public static void main(String[] args)
            throws DocumentListException, IOException, ServiceException,
            InterruptedException {
        SimpleCommandLineParser parser = new SimpleCommandLineParser(args);
        String authSub = parser.getValue("authSub", "auth", "a");
        String user = parser.getValue("username", "user", "u");
        String password = parser.getValue("password", "pass", "p");
        String host = parser.getValue("host", "s");
        boolean help = parser.containsKey("help", "h");

        if (host == null) {
            host = DocumentList.DEFAULT_HOST;
        }

        if (help || (user == null || password == null) && authSub == null) {
            printMessage(USAGE_MESSAGE);
            System.exit(1);
        }

        if (parser.containsKey("log", "l")) {
            turnOnLogging();
        }

        GoogleDocMigrationDemo demo = new GoogleDocMigrationDemo(System.out, APPLICATION_NAME,
                host);

        if (password != null) {
            demo.login(user, password);
        } else {
            demo.login(authSub);
        }

        demo.run();
    }
}