package org.uiautomation.ios.server.simulator;

import org.openqa.selenium.WebDriverException;
import org.uiautomation.ios.IOSCapabilities;
import org.uiautomation.ios.communication.device.DeviceType;
import org.uiautomation.ios.communication.device.DeviceVariation;
import org.uiautomation.ios.server.RealDevice;
import org.uiautomation.ios.server.application.APPIOSApplication;
import org.uiautomation.ios.server.application.IPAApplication;
import org.uiautomation.ios.server.instruments.IOSDeviceManager;
import org.uiautomation.iosdriver.ApplicationInfo;
import org.uiautomation.iosdriver.services.DeviceInstallerService;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class IOSRealDeviceManager implements IOSDeviceManager {

  private static final Logger log = Logger.getLogger(IOSRealDeviceManager.class.getName());
  private final RealDevice device;
  private final IOSCapabilities capabilities;
  private final IPAApplication app;
  private final DeviceInstallerService service;
  private final String bundleId;
  private final List<String> keysToConsiderInThePlistToHaveEquality;

  /**
   * @param capabilities the capability requested by the user.
   * @param device       the device that will host the test
   * @param app          the app that will be ran.
   */
  public IOSRealDeviceManager(IOSCapabilities capabilities, RealDevice device, IPAApplication app) {
    this.device = device;
    bundleId = capabilities.getBundleId();
    this.capabilities = capabilities;
    this.app = app;
    this.service = new DeviceInstallerService(device.getUuid());

    keysToConsiderInThePlistToHaveEquality = new ArrayList<String>();
    keysToConsiderInThePlistToHaveEquality.add("CFBundleVersion");
  }

  @Override
  public void install(APPIOSApplication aut) {
    if (aut instanceof IPAApplication) {
      ApplicationInfo app = service.getApplication(bundleId);
      // not installed ? install the app.
      if (app == null) {
        service.install(((IPAApplication) aut).getIPAFile());
        return;
      }

      // already there and correct version
      if (isCorrectVersion(app)) {
        return;
      }

      // TODO upgrade ?
      // needs to re-install
      log.fine("uninstalling " + bundleId + " for " + device.getUuid());
      service.uninstall(bundleId);
      log.fine("installing " + bundleId + " for " + device.getUuid());
      service.install(((IPAApplication) aut).getIPAFile());
      log.fine(bundleId + " for " + service.getDeviceId() + " installed.");
    } else {
      throw new WebDriverException("only IPA apps can be used on a real device.");
    }
  }

  /**
   * validate the the application with the same bundleId on the device has the correct version, and
   * return true if it does. It currently only checks the the app is "the same" by checking the
   * CFBundleVersion. It should check more than that. Maybe all the magic capabilities ?
   */
  // TODO freynaud should be more strict than that and check all magic keys.
  private boolean isCorrectVersion(ApplicationInfo app) {
    for (String key : keysToConsiderInThePlistToHaveEquality) {
      Object value = app.getProperty(key);
      Object exp = this.app.getMetadata().opt(key);
      if (value instanceof String) {
        if (!value.equals(exp)) {
          log.fine("for key " + key + ",device has" + value + ",but needed " + exp
                   + ".the app will need to be reinstalled.");
          return false;
        }
      }
    }
    return true;
  }


  @Override
  public void setL10N(String locale, String language) {
    service.setLanguage("fr");
    service.setLocale(locale);
  }

  @Override
  public void resetContentAndSettings() {
    service.emptyApplicationCache("com.ebay.iphone");
  }

  @Override
  public void cleanupDevice() {
    // no-op
  }

  @Override
  public void setKeyboardOptions() {
    // no-op
  }

  @Override
  public void setMobileSafariOptions() {
    // no-op
  }

  @Override
  public void setLocationPreference(boolean authorized) {
    // no-op
  }

  @Override
  public void setVariation(DeviceType device, DeviceVariation variation) {
    // no-op
  }

  @Override
  public String getInstrumentsClient() {
    return "instruments";
  }
}
