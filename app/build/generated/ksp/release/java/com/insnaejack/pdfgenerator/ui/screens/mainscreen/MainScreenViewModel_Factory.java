package com.insnaejack.pdfgenerator.ui.screens.mainscreen;

import android.app.Application;
import com.insnaejack.pdfgenerator.billing.BillingClientWrapper;
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
public final class MainScreenViewModel_Factory implements Factory<MainScreenViewModel> {
  private final Provider<Application> applicationProvider;

  private final Provider<BillingClientWrapper> billingClientWrapperProvider;

  public MainScreenViewModel_Factory(Provider<Application> applicationProvider,
      Provider<BillingClientWrapper> billingClientWrapperProvider) {
    this.applicationProvider = applicationProvider;
    this.billingClientWrapperProvider = billingClientWrapperProvider;
  }

  @Override
  public MainScreenViewModel get() {
    return newInstance(applicationProvider.get(), billingClientWrapperProvider.get());
  }

  public static MainScreenViewModel_Factory create(Provider<Application> applicationProvider,
      Provider<BillingClientWrapper> billingClientWrapperProvider) {
    return new MainScreenViewModel_Factory(applicationProvider, billingClientWrapperProvider);
  }

  public static MainScreenViewModel newInstance(Application application,
      BillingClientWrapper billingClientWrapper) {
    return new MainScreenViewModel(application, billingClientWrapper);
  }
}
