package patterns;

import java.time.LocalDateTime;
import java.util.function.BiFunction;

public class ApplicationDesign {

    public static void main(String[] args) {

        Repo repo = new Repo();

        KeyGenStrategy keyGen = (prefix, key) -> prefix + "." + key;

        // Any of below can be used instead of usual keyGen:
        // KeyGenStrategy keyGen1 = (prefix, key) -> prefix + ":" + key;
        // KeyGenStrategy keyGen2 = (suffix, key) -> key + "-" + suffix;

        Service netflixService = new ServiceA(repo, keyGen);
        Service rokuService = new ServiceB(repo, keyGen);

        System.out.println("Netflix: " + netflixService.findByKey("product.key.com"));
        System.out.println("Roku: " + rokuService.findByKey("product.key.com"));
    }

    public static class ServiceA extends Service {

        ServiceA(Repo repo, KeyGenStrategy keyGen) {
            super("nx", repo, keyGen);
        }
    }

    public static class ServiceB extends Service {

        ServiceB(Repo repo, KeyGenStrategy keyGen) {
            super("rk", repo, keyGen);
        }
    }

    interface KeyGenStrategy extends BiFunction<String, String, String> {}

    static abstract class Service {

        final String prefix;
        final Repo repo;
        final KeyGenStrategy keyGen;

        Service(String prefix, Repo repo, KeyGenStrategy keyGen) {
            this.prefix = prefix;
            this.repo = repo;
            this.keyGen = keyGen;
        }

        String findByKey(String key) {
            return repo.get(keyGen.apply(prefix, key));
        }
    }

    public static class Repo {

        String get(String k) {
            return "key: " + k + ", value: " + LocalDateTime.now();
        }
    }

}
