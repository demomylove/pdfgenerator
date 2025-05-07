package com.insnaejack.pdfgenerator.google;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class GoogleDriveService_Factory implements Factory<GoogleDriveService> {
  private final Provider<Context> contextProvider;

  public GoogleDriveService_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public GoogleDriveService get() {
    return newInstance(contextProvider.get());
  }

  public static GoogleDriveService_Factory create(Provider<Context> contextProvider) {
    return new GoogleDriveService_Factory(contextProvider);
  }

  public static GoogleDriveService newInstance(Context context) {
    return new GoogleDriveService(context);
  }
}
