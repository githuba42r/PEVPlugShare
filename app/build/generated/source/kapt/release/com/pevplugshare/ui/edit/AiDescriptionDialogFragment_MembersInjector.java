package com.pevplugshare.ui.edit;

import com.pevplugshare.ai.LocationDescriptionGenerator;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class AiDescriptionDialogFragment_MembersInjector implements MembersInjector<AiDescriptionDialogFragment> {
  private final Provider<LocationDescriptionGenerator> descriptionGeneratorProvider;

  public AiDescriptionDialogFragment_MembersInjector(
      Provider<LocationDescriptionGenerator> descriptionGeneratorProvider) {
    this.descriptionGeneratorProvider = descriptionGeneratorProvider;
  }

  public static MembersInjector<AiDescriptionDialogFragment> create(
      Provider<LocationDescriptionGenerator> descriptionGeneratorProvider) {
    return new AiDescriptionDialogFragment_MembersInjector(descriptionGeneratorProvider);
  }

  @Override
  public void injectMembers(AiDescriptionDialogFragment instance) {
    injectDescriptionGenerator(instance, descriptionGeneratorProvider.get());
  }

  @InjectedFieldSignature("com.pevplugshare.ui.edit.AiDescriptionDialogFragment.descriptionGenerator")
  public static void injectDescriptionGenerator(AiDescriptionDialogFragment instance,
      LocationDescriptionGenerator descriptionGenerator) {
    instance.descriptionGenerator = descriptionGenerator;
  }
}
