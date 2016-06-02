package org.yocto.crops.poky.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ TestFailure.class, TestSuccess.class })
public class AllTests {

}
