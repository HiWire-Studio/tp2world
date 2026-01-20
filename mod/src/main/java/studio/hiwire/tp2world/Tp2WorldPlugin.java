package studio.hiwire.tp2world;

import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import studio.hiwire.tp2world.command.Tp2WorldCommand;
import studio.hiwire.tp2world.config.Tp2WorldConfig;
import studio.hiwire.tp2world.util.TranslationFileManager;

public class Tp2WorldPlugin extends JavaPlugin {

  private static Tp2WorldPlugin INSTANCE;
  public static final String PREFIX = "[HiWire:Tp2World]";
  private static final Path OVERRIDES_PATH = Path.of("overrides");
  private static final List<String> TRANSLATION_FILES =
      List.of("HiWire.Tp2World.ChatMessages.lang");
  private static final List<String> SUPPORTED_LANGUAGES = List.of("en-US", "de-DE");

  private final Config<Tp2WorldConfig> config = withConfig(Tp2WorldConfig.CODEC);

  public Tp2WorldPlugin(@NonNullDecl JavaPluginInit init) {
    super(init);
    // Merge default translations with user's overrides for all languages
    mergeAllTranslations();
  }

  @Override
  protected void setup() {
    INSTANCE = this;

    // Save config (creates default if not exists)
    try {
      config.save().get();
    } catch (InterruptedException | ExecutionException e) {
      getLogger().at(Level.WARNING).withCause(e).log("Failed to save config");
    }

    // Register commands
    getCommandRegistry().registerCommand(new Tp2WorldCommand());
  }

  @Override
  protected void start0() {
    super.start0();

    // Register overrides folder as asset pack for user customizatio
    final var overridesDir = getDataDirectory().resolve(OVERRIDES_PATH);
    if (Files.exists(overridesDir)) {
      AssetModule.get().registerPack(getIdentifier() + "_overrides", overridesDir, getManifest());
      getLogger().at(Level.INFO).log("Registered overrides asset pack from %s", overridesDir);
    }
  }

  @Override
  protected void shutdown() {}

  public static Tp2WorldPlugin get() {
    return INSTANCE;
  }

  public Tp2WorldConfig getConfig() {
    return config.get();
  }

  private void mergeAllTranslations() {
    TranslationFileManager fileManager = new TranslationFileManager(getClass().getClassLoader());

    for (String language : SUPPORTED_LANGUAGES) {
      for (String file : TRANSLATION_FILES) {
        String resourcePath = String.format("Server/Languages/%s/%s", language, file);
        Path targetPath =
            getDataDirectory()
                .resolve(OVERRIDES_PATH)
                .resolve("Server")
                .resolve("Languages")
                .resolve(language)
                .resolve(file);

        TranslationFileManager.MergeResult result = fileManager.merge(resourcePath, targetPath);

        Level level = result.isSuccess() ? Level.INFO : Level.WARNING;
        if (result.status() != TranslationFileManager.MergeResult.Status.NO_CHANGES) {
          getLogger().at(level).log(result.message());
        }
      }
    }
  }
}
