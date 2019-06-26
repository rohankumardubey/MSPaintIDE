package com.uddernetworks.mspaint.code.languages.java;

import com.uddernetworks.mspaint.code.BuildSettings;
import com.uddernetworks.mspaint.code.ImageClass;
import com.uddernetworks.mspaint.code.execution.CompilationResult;
import com.uddernetworks.mspaint.code.execution.DefaultCompilationResult;
import com.uddernetworks.mspaint.code.languages.Language;
import com.uddernetworks.mspaint.code.languages.LanguageSettings;
import com.uddernetworks.mspaint.code.languages.Option;
import com.uddernetworks.mspaint.code.lsp.LSP;
import com.uddernetworks.mspaint.code.lsp.LanguageServerWrapper;
import com.uddernetworks.mspaint.imagestreams.ImageOutputStream;
import com.uddernetworks.mspaint.main.MainGUI;
import com.uddernetworks.mspaint.main.StartupLogic;
import com.uddernetworks.mspaint.project.ProjectManager;
import com.uddernetworks.mspaint.util.IDEFileUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.Consumer;

public class JavaLanguage extends Language {

    private static Logger LOGGER = LoggerFactory.getLogger(JavaLanguage.class);

    private JavaSettings settings = new JavaSettings();
    private JavaCodeManager javaCodeManager = new JavaCodeManager();
    private Map<String, Map<String, String>> replaceData = new HashMap<>();
    private LanguageServerWrapper lspWrapper = new LanguageServerWrapper(this.startupLogic, LSP.JAVA, "E:\\MSPaintIDE\\jdt-language-server-latest",
            Arrays.asList(
                    "java",
                    "-Declipse.application=org.eclipse.jdt.ls.core.id1",
                    "-Dosgi.bundles.defaultStartLevel=4",
                    "-Declipse.product=org.eclipse.jdt.ls.core.product",
                    "-Dlog.level=ALL",
                    "-noverify",
                    "-Xmx1G",
                    "-jar",
                    "%server-path%\\plugins\\org.eclipse.equinox.launcher_1.5.400.v20190515-0925.jar",
                    "-configuration",
                    "%server-path%\\config_win",
                    "-data"
            ), (wrapper, workspaceDir) -> {
        LOGGER.info("Setting up the Java project...");

        // TODO: Remove hardcoding
        var templateDir = new File("C:\\Program Files (x86)\\MS Paint IDE\\lsp\\java\\project-template");

        if (!new File(workspaceDir.getAbsolutePath(), ".classpath").exists()) {
            LOGGER.info("Copying template files...");

            try {
//            FileUtils.copyDirectory(new File(StartupLogic.getJarParent().orElse(new File("")), "lsp\\java\\project-template"), workspaceDir);
                FileUtils.copyDirectory(templateDir, workspaceDir);
            } catch (IOException e) {
                LOGGER.error("An error occurred while copying over project template files!", e);
            }
        } else {
            LOGGER.info("Project already contains template files, no need to copy them again.");
        }

        try {
            var corePrefs = new File(workspaceDir, ".settings\\org.eclipse.jdt.core.prefs").toPath();
            var corePrefsTemplate = new File(templateDir, ".settings\\org.eclipse.jdt.core.prefs").toPath();

            var sourceListener = bindFileVariable(corePrefsTemplate, corePrefs, "replace.versionnumber");
            getLanguageSettings().onChangeSetting(JavaOptions.JAVA_VERSION, (Consumer<String>) value -> sourceListener.accept(value.substring(5)), true);

            var classpath = new File(workspaceDir, ".classpath").toPath();
            var classpathTemplate = new File(templateDir, ".classpath").toPath();

            var versionListener = bindFileVariable(classpathTemplate, classpath, "replace.version");
            getLanguageSettings().onChangeSetting(JavaOptions.JAVA_VERSION, (Consumer<String>) value -> versionListener.accept("JavaSE-" + value.substring(5)), true);

            var srcListener = bindFileVariable(classpathTemplate, classpath, "replace.src");
            getLanguageSettings().onChangeSetting(JavaOptions.INPUT_DIRECTORY, (Consumer<File>) file -> srcListener.accept(relativizeFromBase(file)), true);

            var binListener = bindFileVariable(classpathTemplate, classpath, "replace.bin");
            getLanguageSettings().onChangeSetting(JavaOptions.CLASS_OUTPUT, (Consumer<File>) file -> binListener.accept(relativizeFromBase(file)), true);

            var project = new File(workspaceDir, ".project").toPath();
            var projectTemplate = new File(templateDir, ".project").toPath();

            // TODO: Fix if a method to change project names is added
            LOGGER.info("Replacing to name {}", ProjectManager.getPPFProject().getName());
            bindFileVariable(projectTemplate, project, "replace.name").accept(ProjectManager.getPPFProject().getName());
        } catch (Exception e) { // Caught due to error suppression in the lambdas
            LOGGER.error("There was an exception while writing replacement values for files", e);
        }
    });

    private String relativizeFromBase(File file) {
        return ProjectManager.getPPFProject().getFile().getParentFile().toURI().relativize(file.toURI()).toString();
    }

    private Consumer<String> bindFileVariable(Path input, Path output, String variableName) {
        var fileVariables = this.replaceData.computeIfAbsent(input.toString(), i -> new HashMap<>());
        return newValue -> {
            try {
                fileVariables.put(variableName, newValue);
                String[] content = {new String(Files.readAllBytes(input))};
                fileVariables.forEach((variable, value) -> content[0] = content[0].replace("%" + variable + "%", value));
                Files.write(output, content[0].getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                LOGGER.error("There was a problem writing to " + output.toString(), e);
            }
        };
    }

    public JavaLanguage(StartupLogic startupLogic) {
        super(startupLogic);
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    @Override
    public String getName() {
        return "Java";
    }

    @Override
    public String[] getFileExtensions() {
        return new String[]{"java"};
    }

    @Override
    public Option getInputOption() {
        return JavaOptions.INPUT_DIRECTORY;
    }

    @Override
    public File getInputLocation() {
        return getLanguageSettings().getSetting(JavaOptions.INPUT_DIRECTORY);
    }

    @Override
    public File getAppOutput() {
        return getLanguageSettings().getSetting(JavaOptions.PROGRAM_OUTPUT);
    }

    @Override
    public File getCompilerOutput() {
        return getLanguageSettings().getSetting(JavaOptions.COMPILER_OUTPUT);
    }

    @Override
    public boolean isInterpreted() {
        return false;
    }

    @Override
    public LanguageServerWrapper getLSPWrapper() {
        return this.lspWrapper;
    }

    @Override
    public boolean hasLSP() {
        return true;
    }

    @Override
    public void installLSP(Consumer<Boolean> successful) {

    }

    @Override
    public boolean hasRuntime() {
        return false;
    }

    @Override
    public String downloadRuntimeLink() {
        return null;
    }

    @Override
    public String getLanguageHighlighter() {
        return null;
    }

    @Override
    public LanguageSettings getLanguageSettings() {
        return this.settings;
    }

    @Override
    public void highlightAll(List<ImageClass> imageClasses) throws IOException {
        if (!this.settings.<Boolean>getSetting(JavaOptions.HIGHLIGHT)) return;
        highlightAll(JavaOptions.HIGHLIGHT_DIRECTORY, imageClasses);
    }

    @Override
    public Optional<List<ImageClass>> indexFiles() {
        return indexFiles(JavaOptions.INPUT_DIRECTORY);
    }

    @Override
    public CompilationResult compileAndExecute(MainGUI mainGUI, ImageOutputStream imageOutputStream, ImageOutputStream compilerStream) throws IOException {
        var imageClassesOptional = indexFiles();
        if (imageClassesOptional.isEmpty()) {
            LOGGER.error("Error while finding ImageClasses, aborting...");
            return new DefaultCompilationResult(CompilationResult.Status.COMPILE_COMPLETE);
        }

        return compileAndExecute(mainGUI, imageClassesOptional.get(), imageOutputStream, compilerStream);
    }

    @Override
    public CompilationResult compileAndExecute(MainGUI mainGUI, List<ImageClass> imageClasses, ImageOutputStream imageOutputStream, ImageOutputStream compilerStream) throws IOException {
        return compileAndExecute(mainGUI, imageClasses, imageOutputStream, compilerStream, BuildSettings.DEFAULT);
    }

    @Override
    public CompilationResult compileAndExecute(MainGUI mainGUI, List<ImageClass> imageClasses, ImageOutputStream imageOutputStream, ImageOutputStream compilerStream, BuildSettings executeOverride) throws IOException {
        var jarFile = this.settings.<File>getSetting(JavaOptions.JAR);
        var libDirectoryOptional = this.settings.<File>getSettingOptional(JavaOptions.LIBRARY_LOCATION);
        var otherFilesOptional = this.settings.<File>getSettingOptional(JavaOptions.OTHER_LOCATION);
        var classOutput = this.settings.<File>getSetting(JavaOptions.CLASS_OUTPUT);
        var execute = false;
        if (executeOverride == BuildSettings.EXECUTE) {
            execute = true;
        } else if (executeOverride != BuildSettings.DONT_EXECUTE) {
            execute = this.settings.<Boolean>getSetting(JavaOptions.EXECUTE);
        }

        var libFiles = new ArrayList<File>();
        libDirectoryOptional.ifPresent(libDirectory -> {
            if (libDirectory.isFile()) {
                if (libDirectory.getName().endsWith(".jar")) libFiles.add(libDirectory);
            } else {
                libFiles.addAll(IDEFileUtils.getFilesFromDirectory(libDirectory, "jar"));
            }
        });

        return this.javaCodeManager.compileAndExecute(imageClasses, jarFile, otherFilesOptional.orElse(null), classOutput, mainGUI, imageOutputStream, compilerStream, libFiles, execute);
    }

    @Override
    public String toString() {
        return getName();
    }
}
