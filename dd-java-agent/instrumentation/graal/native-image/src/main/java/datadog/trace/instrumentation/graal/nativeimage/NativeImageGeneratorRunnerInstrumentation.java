package datadog.trace.instrumentation.graal.nativeimage;

import static datadog.trace.agent.tooling.bytebuddy.matcher.NameMatchers.named;
import static datadog.trace.api.config.GeneralConfig.SERVICE_NAME;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;

import com.google.auto.service.AutoService;
import datadog.trace.agent.tooling.Instrumenter;
import datadog.trace.agent.tooling.InstrumenterModule;
import datadog.trace.api.InstrumenterConfig;
import datadog.trace.api.env.CapturedEnvironment;
import java.util.Arrays;
import java.util.List;
import net.bytebuddy.asm.Advice;

@AutoService(InstrumenterModule.class)
public final class NativeImageGeneratorRunnerInstrumentation
    extends AbstractNativeImageInstrumentation implements Instrumenter.ForSingleType {

  @Override
  public String instrumentedType() {
    return "com.oracle.svm.hosted.NativeImageGeneratorRunner";
  }

  @Override
  public void methodAdvice(MethodTransformer transformer) {
    transformer.applyAdvice(
        isMethod().and(named("main")),
        NativeImageGeneratorRunnerInstrumentation.class.getName() + "$ArgsAdvice");
    transformer.applyAdvice(
        isMethod().and(named("extractDriverArguments")),
        NativeImageGeneratorRunnerInstrumentation.class.getName() + "$ExtractedArgsAdvice");
  }

  public static class ArgsAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void onEnter(@Advice.Argument(value = 0, readOnly = false) String[] args) {
      int oldLength = args.length;

      // attempt to extract configured image name, so we can use it for the service name
      for (int i = 0; i < oldLength; i++) {
        if (args[i].startsWith("-H:Name=")) {
          String name = args[i].substring(8);
          CapturedEnvironment.get().getProperties().put(SERVICE_NAME, name);
          break;
        }
      }

      // !!! Important - if you add more args entries here, make sure to update the array copy below
      args =
          Arrays.copyOf(
              args, oldLength + 5 + (InstrumenterConfig.get().isProfilingEnabled() ? 1 : 0));

      args[oldLength++] = "-H:+AddAllCharsets";
      args[oldLength++] = "-H:EnableURLProtocols=http";
      // placeholder to trigger resource scanning, ResourcesFeatureInstrumentation does the rest
      args[oldLength++] = "-H:IncludeResources=.*dd-.*version$";
      args[oldLength++] =
          "-H:ReflectionConfigurationResources="
              + "META-INF/native-image/com.datadoghq/dd-java-agent/reflect-config.json";
      args[oldLength++] =
          "-H:ClassInitialization="
              + "com.datadog.profiling.controller.openjdk.events.AvailableProcessorCoresEvent:build_time,"
              + "com.datadog.profiling.controller.openjdk.events.DeadlockEvent:build_time,"
              + "com.datadog.profiling.controller.openjdk.events.ProfilerSettingEvent:build_time,"
              + "com.datadog.profiling.controller.openjdk.events.EndpointEvent:build_time,"
              + "com.datadog.profiling.controller.openjdk.events.QueueTimeEvent:build_time,"
              + "com.datadog.profiling.controller.openjdk.events.TimelineEvent:build_time,"
              + "com.datadog.profiling.controller.openjdk.events.SmapEntryEvent:build_time,"
              + "com.datadog.profiling.controller.openjdk.events.SmapEntryFactory$SmapParseErrorEvent:build_time,"
              + "datadog.trace.api.Config:rerun,"
              + "datadog.trace.api.Platform:rerun,"
              + "datadog.trace.api.Platform$Captured:build_time,"
              + "datadog.trace.api.env.CapturedEnvironment:build_time,"
              + "datadog.trace.api.ConfigCollector:rerun,"
              + "datadog.trace.api.ConfigDefaults:build_time,"
              + "datadog.trace.api.ConfigSetting:build_time,"
              + "datadog.trace.api.InstrumenterConfig:build_time,"
              + "datadog.trace.api.Functions:build_time,"
              + "datadog.trace.api.GlobalTracer:build_time,"
              + "datadog.trace.api.MethodFilterConfigParser:build_time,"
              + "datadog.trace.api.WithGlobalTracer:build_time,"
              + "datadog.trace.api.PropagationStyle:build_time,"
              + "datadog.trace.api.telemetry.OtelEnvMetricCollector:build_time,"
              + "datadog.trace.api.profiling.ProfilingEnablement:build_time,"
              + "datadog.trace.bootstrap.config.provider.ConfigConverter:build_time,"
              + "datadog.trace.bootstrap.config.provider.ConfigProvider:build_time,"
              + "datadog.trace.bootstrap.config.provider.ConfigProvider$Singleton:build_time,"
              + "datadog.trace.bootstrap.config.provider.OtelEnvironmentConfigSource:build_time,"
              + "datadog.trace.bootstrap.Agent:build_time,"
              + "datadog.trace.bootstrap.BootstrapProxy:build_time,"
              + "datadog.trace.bootstrap.CallDepthThreadLocalMap:build_time,"
              + "datadog.trace.bootstrap.DatadogClassLoader:build_time,"
              + "datadog.trace.bootstrap.InstrumentationClassLoader:build_time,"
              + "datadog.trace.bootstrap.FieldBackedContextStores:build_time,"
              + "datadog.trace.bootstrap.benchmark.StaticEventLogger:build_time,"
              + "datadog.trace.bootstrap.blocking.BlockingExceptionHandler:build_time,"
              + "datadog.trace.bootstrap.InstrumentationErrors:build_time,"
              + "datadog.trace.bootstrap.instrumentation.java.concurrent.ConcurrentState:build_time,"
              + "datadog.trace.bootstrap.instrumentation.java.concurrent.ExcludeFilter:build_time,"
              + "datadog.trace.bootstrap.instrumentation.java.concurrent.QueueTimeHelper:build_time,"
              + "datadog.trace.bootstrap.instrumentation.java.concurrent.TPEHelper:build_time,"
              + "datadog.trace.bootstrap.instrumentation.jfr.exceptions.ExceptionCountEvent:build_time,"
              + "datadog.trace.bootstrap.instrumentation.jfr.exceptions.ExceptionSampleEvent:build_time,"
              + "datadog.trace.bootstrap.instrumentation.jfr.backpressure.BackpressureSampleEvent:build_time,"
              + "datadog.trace.bootstrap.instrumentation.jfr.directallocation.DirectAllocationTotalEvent:build_time,"
              + "datadog.trace.logging.LoggingSettingsDescription:build_time,"
              + "datadog.trace.logging.simplelogger.SLCompatFactory:build_time,"
              + "datadog.trace.logging.LogReporter:build_time,"
              + "datadog.trace.logging.PrintStreamWrapper:build_time,"
              + "datadog.trace.util.CollectionUtils:build_time,"
              + "datadog.slf4j.impl.StaticLoggerBinder:build_time,"
              + "datadog.slf4j.LoggerFactory:build_time,"
              + "com.blogspot.mydailyjava.weaklockfree.WeakConcurrentMap:build_time,"
              + "net.bytebuddy:build_time,"
              + "com.sun.proxy:build_time,"
              + "jnr.enxio.channels:run_time,"
              + "jnr.unixsocket:run_time";
      if (InstrumenterConfig.get().isProfilingEnabled()) {
        // Specific GraalVM versions have different flags for enabling JFR
        // We don't want to drag in internal-api via Platform class, so we just read the system
        // property directly
        String version = System.getProperty("java.specification.version");
        if (version.startsWith("17")) {
          args[oldLength++] = "-H:EnableMonitoringFeatures=jfr";
        } else {
          args[oldLength++] = "-H:EnableMonitoringFeatures@user+api=jfr";
        }
      }
    }
  }

  public static class ExtractedArgsAdvice {
    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onExit(@Advice.Return List<String> expandedArgs) {
      // GraalVM 22.x supports arg files, so repeat image name check after expansion
      for (int i = 0; i < expandedArgs.size(); i++) {
        if (expandedArgs.get(i).startsWith("-H:Name=")) {
          String name = expandedArgs.get(i).substring(8);
          CapturedEnvironment.get().getProperties().put(SERVICE_NAME, name);
          break;
        }
      }
    }
  }
}
