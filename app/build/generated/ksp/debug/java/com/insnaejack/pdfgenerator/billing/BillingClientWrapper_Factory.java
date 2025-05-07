package com.insnaejack.pdfgenerator.billing;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class BillingClientWrapper_Factory implements Factory<BillingClientWrapper> {
  private final Provider<Context> contextProvider;

  public BillingClientWrapper_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public BillingClientWrapper get() {
    return newInstance(contextProvider.get());
  }

  public static BillingClientWrapper_Factory create(Provider<Context> contextProvider) {
    return new BillingClientWrapper_Factory(contextProvider);
  }

  public static BillingClientWrapper newInstance(Context context) {
    return new BillingClientWrapper(context);
  }
}
