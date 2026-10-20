package de.baumann.browser.feature.browser;

import de.baumann.browser.feature.common.HelperUnit;
import de.baumann.browser.feature.data.RecordAction;
import de.baumann.browser.feature.data.RecordUnit;

import android.content.Context;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class List_standard {

    private static final Set<String> domainSet = Collections.synchronizedSet(new HashSet<>());
    private static final AtomicBoolean isLoaded = new AtomicBoolean(false);
    private final Context context;

    public List_standard(Context context) {
        this.context = context.getApplicationContext();
        ensureDomainsLoaded(this.context);
    }

    private static void ensureDomainsLoaded(Context context) {
        if (!isLoaded.get()) {
            synchronized (domainSet) {
                if (!isLoaded.get()) {
                    loadDomains(context);
                    isLoaded.set(true);
                }
            }
        }
    }

    private static void loadDomains(Context context) {
        RecordAction action = new RecordAction(context);
        action.open(false);
        List<String> domains = action.listDomains(RecordUnit.TABLE_STANDARD);
        action.close();
        domainSet.clear();
        if (domains != null) {
            domainSet.addAll(domains);
        }
    }

    public boolean isWhite(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        String domain = HelperUnit.domain(url);
        if (domain == null) {
            return false;
        }
        return domainSet.contains(domain);
    }

    public void addDomain(String domain) {
        if (domain == null || domain.trim().isEmpty()) {
            return;
        }
        RecordAction action = new RecordAction(context);
        action.open(true);
        action.addDomain(domain, RecordUnit.TABLE_STANDARD);
        action.close();
        domainSet.add(domain);
    }

    public void removeDomain(String domain) {
        if (domain == null || domain.trim().isEmpty()) {
            return;
        }
        RecordAction action = new RecordAction(context);
        action.open(true);
        action.deleteDomain(domain, RecordUnit.TABLE_STANDARD);
        action.close();
        domainSet.remove(domain);
    }
}
