/*
 * Copyright (C) 2026 Linagora
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version, provided you comply with the Additional Terms applicable for LinID Identity Manager software by
 * LINAGORA pursuant to Section 7 of the GNU Affero General Public License.
 */

import { shallowMount } from '@vue/test-utils';
import ExportApplicationScriptBtn from 'src/components/btn/ExportApplicationScriptBtn.vue';
import { exportApplicationScript } from 'src/services/ApplicationService';
import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('src/services/ApplicationService', () => ({
  exportApplicationScript: vi.fn(),
}));

const tMock = vi.fn((key) => key);
const uiMock = vi.fn(() => ({}));
const notifyMock = vi.fn();

vi.mock('@linagora/linid-im-front-corelib', () => ({
  useScopedI18n: vi.fn(() => ({
    t: tMock,
  })),
  useUiDesign: vi.fn(() => ({
    ui: uiMock,
  })),
  useNotify: vi.fn(() => ({
    Notify: notifyMock,
  })),
}));

const buildEntity = (overrides = {}) => ({
  id: 'application-id',
  code: 'my-application',
  deployedAt: null,
  ...overrides,
});

describe('Test component: ExportApplicationScriptBtn', () => {
  beforeEach(() => {
    vi.clearAllMocks();

    vi.spyOn(window.URL, 'createObjectURL').mockReturnValue('blob:url');
    vi.spyOn(window.URL, 'revokeObjectURL').mockImplementation(() => {});
  });

  describe('Test function: exportScript', () => {
    it('should export application script and notify success', async () => {
      const blob = new Blob(['package authz']);

      vi.mocked(exportApplicationScript).mockResolvedValue(blob);

      const clickMock = vi.fn();
      const fakeLink = {
        href: '',
        download: '',
        click: clickMock,
        remove: vi.fn(),
      };

      const originalCreateElement = document.createElement.bind(document);
      vi.spyOn(document, 'createElement').mockImplementation((tagName) => {
        if (tagName === 'a') {
          return fakeLink;
        }
        return originalCreateElement(tagName);
      });

      const appendSpy = vi
        .spyOn(document.body, 'appendChild')
        .mockImplementation((node) => node);

      const wrapper = shallowMount(ExportApplicationScriptBtn, {
        props: {
          uiNamespace: 'application',
          i18nScope: 'Application',
          entity: buildEntity({
            code: 'my-app',
          }),
        },
      });

      await wrapper.vm.exportScript();

      expect(exportApplicationScript).toHaveBeenCalledWith(
        buildEntity({
          code: 'my-app',
        })
      );

      expect(clickMock).toHaveBeenCalled();
      expect(fakeLink.href).toBe('blob:url');
      expect(fakeLink.download).toBe('my-app.rego');

      expect(window.URL.createObjectURL).toHaveBeenCalledWith(blob);
      expect(window.URL.revokeObjectURL).toHaveBeenCalledWith('blob:url');

      expect(notifyMock).toHaveBeenCalledWith({
        type: 'positive',
        message: 'export.success',
      });

      appendSpy.mockRestore();
      vi.mocked(document.createElement).mockRestore();
    });

    it('should notify error when export fails', async () => {
      vi.mocked(exportApplicationScript).mockRejectedValue(
        new Error('Export failed')
      );

      const consoleSpy = vi
        .spyOn(console, 'error')
        .mockImplementation(() => {});

      const wrapper = shallowMount(ExportApplicationScriptBtn, {
        props: {
          uiNamespace: 'application',
          i18nScope: 'Application',
          entity: buildEntity(),
        },
      });

      await wrapper.vm.exportScript();

      expect(notifyMock).toHaveBeenCalledWith({
        type: 'negative',
        message: 'export.error',
      });

      expect(consoleSpy).toHaveBeenCalled();

      consoleSpy.mockRestore();
    });
  });
});
