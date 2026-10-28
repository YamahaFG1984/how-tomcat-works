package modern.ch11.core;

import modern.ch06.core.LifecycleException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** 第 8 章的 Context 加上过滤器定义和过滤器映射；启动时为每个定义创建 ApplicationFilterConfig */
public class StandardContext extends modern.ch08.core.SimpleContext {

    private final Map<String, FilterDef> filterDefs = new ConcurrentHashMap<>();
    private final List<FilterMap> filterMaps = new CopyOnWriteArrayList<>();
    private final Map<String, ApplicationFilterConfig> filterConfigs = new ConcurrentHashMap<>();

    public void addFilterDef(FilterDef def) { filterDefs.put(def.filterName(), def); }
    public void addFilterMap(FilterMap map) { filterMaps.add(map); }
    public List<FilterMap> findFilterMaps() { return List.copyOf(filterMaps); }
    public ApplicationFilterConfig findFilterConfig(String name) { return filterConfigs.get(name); }

    @Override
    public synchronized void start() throws LifecycleException {
        super.start();
        filterDefs.values().forEach(def -> filterConfigs.put(def.filterName(), new ApplicationFilterConfig(this, def)));
    }

    @Override
    public synchronized void stop() throws LifecycleException {
        filterConfigs.values().forEach(ApplicationFilterConfig::release);
        filterConfigs.clear();
        super.stop();
    }
}
