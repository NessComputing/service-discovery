/**
 * Copyright (C) 2012 Ness Computing, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nesscomputing.service.discovery.announce;

import java.net.URI;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.nesscomputing.config.Config;
import com.nesscomputing.service.discovery.announce.AnnouncementConfig;
import com.nesscomputing.service.discovery.announce.GalaxyAnnouncementModule;

public class TestGalaxyAnnouncementModule
{
    private Config config = null;
    private Config emptyConfig = null;

    @Before
    public void setUp()
    {
        Assert.assertNull(this.config);
        this.config = Config.getConfig(URI.create("classpath:/test-config"), "galaxy-announce");
        this.emptyConfig = Config.getConfig(URI.create("classpath:/test-config"), "empty");
    }

    @After
    public void tearDown()
    {
        Assert.assertNotNull(this.config);
        this.config = null;
        Assert.assertNotNull(this.emptyConfig);
        this.emptyConfig = null;
    }

    @Test
    public void testSimple()
    {
        final GalaxyAnnouncementModule module = new GalaxyAnnouncementModule();
        final AnnouncementConfig cfg = module.getAnnouncementConfig(config);

        Assert.assertEquals("configured-service", cfg.getServiceName());
        Assert.assertEquals("configured-type", cfg.getServiceType());

        Assert.assertEquals(false, cfg.isAnnounceInternal());
        Assert.assertEquals(true, cfg.isAnnounceExternal());
    }

    @Test
    public void testOverride()
    {
        final GalaxyAnnouncementModule module = new GalaxyAnnouncementModule("override-service", "override-type");
        final AnnouncementConfig cfg = module.getAnnouncementConfig(config);

        Assert.assertEquals("configured-service", cfg.getServiceName());
        Assert.assertEquals("configured-type", cfg.getServiceType());

        Assert.assertEquals(false, cfg.isAnnounceInternal());
        Assert.assertEquals(true, cfg.isAnnounceExternal());
    }

    @Test
    public void testOverrideNullType()
    {
        final GalaxyAnnouncementModule module = new GalaxyAnnouncementModule("override-service", null);
        final AnnouncementConfig cfg = module.getAnnouncementConfig(config);

        Assert.assertEquals("configured-service", cfg.getServiceName());
        Assert.assertEquals("configured-type", cfg.getServiceType());

        Assert.assertEquals(false, cfg.isAnnounceInternal());
        Assert.assertEquals(true, cfg.isAnnounceExternal());
    }

    @Test
    public void testDefaults()
    {
        final GalaxyAnnouncementModule module = new GalaxyAnnouncementModule();
        final AnnouncementConfig cfg = module.getAnnouncementConfig(emptyConfig);

        Assert.assertEquals(null, cfg.getServiceName());
        Assert.assertEquals(null, cfg.getServiceType());

        Assert.assertEquals(true, cfg.isAnnounceInternal());
        Assert.assertEquals(false, cfg.isAnnounceExternal());
    }

    @Test
    public void testOverrideEmpty()
    {
        final GalaxyAnnouncementModule module = new GalaxyAnnouncementModule("override-service", "override-type");
        final AnnouncementConfig cfg = module.getAnnouncementConfig(emptyConfig);

        Assert.assertEquals("override-service", cfg.getServiceName());
        Assert.assertEquals("override-type", cfg.getServiceType());

        Assert.assertEquals(true, cfg.isAnnounceInternal());
        Assert.assertEquals(false, cfg.isAnnounceExternal());
    }

    @Test
    public void testOverrideEmptyNullType()
    {
        final GalaxyAnnouncementModule module = new GalaxyAnnouncementModule("override-service", null);
        final AnnouncementConfig cfg = module.getAnnouncementConfig(emptyConfig);

        Assert.assertEquals("override-service", cfg.getServiceName());
        Assert.assertEquals(null, cfg.getServiceType());

        Assert.assertEquals(true, cfg.isAnnounceInternal());
        Assert.assertEquals(false, cfg.isAnnounceExternal());
    }
}
