package com.insnaejack.pdfgenerator.ui.screens.googledrive;

import com.insnaejack.pdfgenerator.google.GoogleDriveService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
    "KotlinInternalInJava",
    "cast"
})
public final class GoogleDriveViewModel_Factory implements Factory<GoogleDriveViewModel> {
  private final Provider<GoogleDriveService> driveServiceProvider;

  public GoogleDriveViewModel_Factory(Provider<GoogleDriveService> driveServiceProvider) {
    this.driveServiceProvider = driveServiceProvider;
  }

  @Override
  public GoogleDriveViewModel get() {
    return newInstance(driveServiceProvider.get());
  }

  public static GoogleDriveViewModel_Factory create(
      Provider<GoogleDriveService> driveServiceProvider) {
    return new GoogleDriveViewModel_Factory(driveServiceProvider);
  }

  public static GoogleDriveViewModel newInstance(GoogleDriveService driveService) {
    return new GoogleDriveViewModel(driveService);
  }
}
