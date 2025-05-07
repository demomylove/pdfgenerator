package com.insnaejack.pdfgenerator.di;

import android.content.Context;
import com.insnaejack.pdfgenerator.billing.BillingClientWrapper;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class BillingModule_ProvideBillingClientWrapperFactory implements Factory<BillingClientWrapper> {
  private final Provider<Context> contextProvider;

  public BillingModule_ProvideBillingClientWrapperFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public BillingClientWrapper get() {
    return provideBillingClientWrapper(contextProvider.get());
  }

  public static BillingModule_ProvideBillingClientWrapperFactory create(
      Provider<Context> contextProvider) {
    return new BillingModule_ProvideBillingClientWrapperFactory(contextProvider);
  }

  public static BillingClientWrapper provideBillingClientWrapper(Context context) {
    return Preconditions.checkNotNullFromProvides(BillingModule.INSTANCE.provideBillingClientWrapper(context));
  }
}
